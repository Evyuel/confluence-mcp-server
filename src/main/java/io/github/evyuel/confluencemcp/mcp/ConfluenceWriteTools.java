package io.github.evyuel.confluencemcp.mcp;

import io.github.evyuel.confluencemcp.dto.CreatePageResponse;
import io.github.evyuel.confluencemcp.service.ConfluenceWriteService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConfluenceWriteTools {
    private final ConfluenceWriteService service;

    public ConfluenceWriteTools(ConfluenceWriteService service) {
        this.service = service;
    }

    @McpTool(name = "create_page", description = "Create one published page directly below the server-configured parent. Content must be Confluence STORAGE XHTML; destination is not user-selectable.")
    public CreatePageResponse createPage(
            @McpToolParam(description = "Requested page title") String title,
            @McpToolParam(description = "Confluence STORAGE XHTML content") String content) {
        return service.createPage(title, content);
    }
}
