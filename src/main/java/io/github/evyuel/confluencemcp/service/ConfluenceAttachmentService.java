package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.dto.AttachmentsResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ConfluenceAttachmentService {
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final ConfluenceClient client;
    private final ConfluenceProperties properties;

    public ConfluenceAttachmentService(ConfluenceClient client, ConfluenceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public AttachmentsResponse attachments(String pageId, Integer limit, Integer start) {
        int actualStart = ConfluenceJson.normalizeStart(start);
        int actualLimit = ConfluenceJson.normalizeLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        try {
            JsonNode root = client.listAttachments(pageId, actualStart, actualLimit);
            var attachments = new ArrayList<AttachmentsResponse.Attachment>();
            for (JsonNode item : root.path("results")) {
                String download = ConfluenceJson.text(item, "/_links/download");
                if (download != null && !download.startsWith("http")) {
                    download = properties.baseUrl().toString().replaceAll("/$", "")
                            + (download.startsWith("/") ? download : "/" + download);
                }
                attachments.add(new AttachmentsResponse.Attachment(
                        ConfluenceJson.text(item, "/id"), ConfluenceJson.text(item, "/title"),
                        ConfluenceJson.text(item, "/metadata/mediaType"),
                        ConfluenceJson.longValue(item, "/extensions/fileSize"),
                        ConfluenceJson.integer(item, "/version/number"), download,
                        ConfluenceJson.text(item, "/metadata/comment"),
                        ConfluenceJson.text(item, "/history/createdDate"),
                        ConfluenceJson.text(item, "/version/when")));
            }
            return new AttachmentsResponse(pageId, attachments,
                    ConfluenceJson.pagination(root, actualStart, actualLimit, attachments.size()));
        } catch (RuntimeException ex) {
            throw ServiceErrors.attachments(ex);
        }
    }
}
