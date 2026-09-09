package io.github.evyuel.confluencemcp.mcp;

import io.github.evyuel.confluencemcp.dto.PageFormat;
import io.github.evyuel.confluencemcp.dto.PageResponse;
import io.github.evyuel.confluencemcp.service.ConfluencePageService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConfluencePageTools {
    private final ConfluencePageService service;

    public ConfluencePageTools(ConfluencePageService service) {
        this.service = service;
    }

    @McpTool(name = "get_page", description = "Read a current Confluence page in chunks as Markdown or original Confluence STORAGE XHTML. Returns page content, not a summary.")
    public PageResponse getPage(
            @McpToolParam(description = "Confluence numeric page ID") String pageId,
            @McpToolParam(required = false, description = "MARKDOWN (default) or STORAGE") PageFormat format,
            @McpToolParam(required = false, description = "Character offset; start at 0 and follow nextOffset") Integer offset,
            @McpToolParam(required = false, description = "Maximum characters for this chunk; server-capped") Integer maxChars) {
        return service.getPage(pageId, format, offset, maxChars);
    }
}
