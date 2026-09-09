package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.dto.CreatePageResponse;
import io.github.evyuel.confluencemcp.exception.ConfluenceHttpException;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ConfluenceWriteService {
    static final int MAX_RENAMED_ATTEMPTS = 100;

    private final ConfluenceClient client;
    private final ConfluenceProperties properties;
    private final ObjectMapper objectMapper;

    public ConfluenceWriteService(ConfluenceClient client, ConfluenceProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public CreatePageResponse createPage(String title, String content) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content must not be blank");
        long actualBytes = content.getBytes(StandardCharsets.UTF_8).length;
        long maximumBytes = properties.write().maxContentSize().toBytes();
        if (actualBytes > maximumBytes) {
            throw new ConfluenceToolException("Content size " + humanSize(actualBytes)
                    + " exceeds configured maximum " + humanSize(maximumBytes)
                    + ". Reduce the content or split the documentation into multiple pages.");
        }

        String parentId = properties.write().parentPageId();
        final String spaceKey;
        try {
            JsonNode parent = client.getPage(parentId, "space");
            spaceKey = ConfluenceJson.text(parent, "/space/key");
            if (spaceKey == null || spaceKey.isBlank()) {
                throw new ConfluenceToolException("Configured parent page has no accessible Space.");
            }
        } catch (RuntimeException ex) {
            throw ServiceErrors.page(ex);
        }

        for (int attempt = 0; attempt <= MAX_RENAMED_ATTEMPTS; attempt++) {
            String actualTitle = attempt == 0 ? title : attempt + " - " + title;
            try {
                JsonNode created = client.createPage(request(parentId, spaceKey, actualTitle, content));
                return new CreatePageResponse(ConfluenceJson.text(created, "/id"), title, actualTitle,
                        attempt > 0, ConfluenceJson.url(created, properties));
            } catch (ConfluenceHttpException ex) {
                if (!ex.isDuplicateTitle()) {
                    throw new ConfluenceToolException("Confluence rejected create_page: " + ex.getMessage(), ex);
                }
            }
        }
        throw new ConfluenceToolException("Could not find a unique title after " + MAX_RENAMED_ATTEMPTS
                + " conflict-renaming attempts.");
    }

    private ObjectNode request(String parentId, String spaceKey, String title, String content) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "page");
        root.put("status", "current");
        root.put("title", title);
        root.putArray("ancestors").addObject().put("id", parentId);
        root.putObject("space").put("key", spaceKey);
        ObjectNode storage = root.putObject("body").putObject("storage");
        storage.put("value", content);
        storage.put("representation", "storage");
        return root;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        double mib = bytes / (1024.0 * 1024.0);
        String value = mib == Math.rint(mib) ? String.format("%.0f", mib) : String.format("%.2f", mib);
        return value + " MiB";
    }
}
