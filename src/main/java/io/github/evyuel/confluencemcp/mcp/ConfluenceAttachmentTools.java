package io.github.evyuel.confluencemcp.mcp;

import io.github.evyuel.confluencemcp.dto.AttachmentsResponse;
import io.github.evyuel.confluencemcp.service.ConfluenceAttachmentService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConfluenceAttachmentTools {
    private final ConfluenceAttachmentService service;

    public ConfluenceAttachmentTools(ConfluenceAttachmentService service) {
        this.service = service;
    }

    @McpTool(name = "list_page_attachments", description = "List attachment metadata only. Does not download, read, extract, or analyze attachment content.")
    public AttachmentsResponse listPageAttachments(
            @McpToolParam(description = "Confluence numeric page ID") String pageId,
            @McpToolParam(required = false, description = "Page size; defaults to 20 and is capped at 50") Integer limit,
            @McpToolParam(required = false, description = "Zero-based result offset; defaults to 0") Integer start) {
        return service.attachments(pageId, limit, start);
    }
}
