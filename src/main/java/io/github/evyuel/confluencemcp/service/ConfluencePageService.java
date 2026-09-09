package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.converter.ConfluenceContentConverter;
import io.github.evyuel.confluencemcp.converter.ConversionResult;
import io.github.evyuel.confluencemcp.dto.PageFormat;
import io.github.evyuel.confluencemcp.dto.PageResponse;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfluencePageService {
    private final ConfluenceClient client;
    private final ConfluenceProperties properties;
    private final ConfluenceContentConverter converter;
    private final ConcurrentHashMap<String, Integer> chunkVersions = new ConcurrentHashMap<>();

    public ConfluencePageService(ConfluenceClient client, ConfluenceProperties properties,
                                 ConfluenceContentConverter converter) {
        this.client = client;
        this.properties = properties;
        this.converter = converter;
    }

    public PageResponse getPage(String pageId, PageFormat format, Integer offset, Integer maxChars) {
        PageFormat actualFormat = format == null ? PageFormat.MARKDOWN : format;
        int actualOffset = offset == null ? 0 : offset;
        if (actualOffset < 0) throw new IllegalArgumentException("offset must not be negative");
        int actualMaxChars = maxChars == null ? properties.read().defaultMaxChars() : maxChars;
        if (actualMaxChars <= 0) throw new IllegalArgumentException("maxChars must be positive");
        actualMaxChars = Math.min(actualMaxChars, properties.read().maxChars());

        try {
            JsonNode page = client.getPage(pageId, "body.storage,version,space,history.lastUpdated");
            int version = page.path("version").path("number").asInt();
            enforceVersionConsistency(pageId, actualFormat, actualOffset, version);

            String storage = ConfluenceJson.text(page, "/body/storage/value");
            if (storage == null) storage = "";
            ConversionResult conversion = actualFormat == PageFormat.MARKDOWN
                    ? converter.toMarkdown(storage) : new ConversionResult(storage, "NOT_APPLICABLE", List.of());
            String fullContent = conversion.content();
            if ("FAILED".equals(conversion.status())) {
                if (actualOffset != 0) {
                    throw new IllegalArgumentException("Markdown conversion failed; retry from offset=0 with format=STORAGE");
                }
                return new PageResponse(pageId, ConfluenceJson.text(page, "/title"),
                        ConfluenceJson.text(page, "/space/key"), ConfluenceJson.url(page, properties), version,
                        ConfluenceJson.text(page, "/history/lastUpdated/when"), actualFormat,
                        fullContent, fullContent.length(), 0, null, false,
                        conversion.status(), conversion.warnings());
            }
            if (actualOffset > fullContent.length()) {
                throw new IllegalArgumentException("offset exceeds totalChars (" + fullContent.length() + ")");
            }
            int end = Math.min(fullContent.length(), actualOffset + actualMaxChars);
            boolean hasMore = end < fullContent.length();
            return new PageResponse(pageId, ConfluenceJson.text(page, "/title"),
                    ConfluenceJson.text(page, "/space/key"), ConfluenceJson.url(page, properties), version,
                    ConfluenceJson.text(page, "/history/lastUpdated/when"), actualFormat,
                    fullContent.substring(actualOffset, end), fullContent.length(), actualOffset,
                    hasMore ? end : null, hasMore, conversion.status(), conversion.warnings());
        } catch (IllegalArgumentException | ConfluenceToolException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw ServiceErrors.page(ex);
        }
    }

    private void enforceVersionConsistency(String pageId, PageFormat format, int offset, int version) {
        String key = pageId + ':' + format;
        if (offset == 0) {
            chunkVersions.put(key, version);
            return;
        }
        Integer expected = chunkVersions.get(key);
        if (expected == null) {
            throw new ConfluenceToolException("Chunk sequence is not initialized. Read this page from offset=0 first.");
        }
        if (expected != version) {
            chunkVersions.put(key, version);
            throw new ConfluenceToolException("Page version changed from " + expected + " to " + version
                    + " during chunked reading. Restart from offset=0.");
        }
    }
}
