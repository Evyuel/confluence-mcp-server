package io.github.evyuel.confluencemcp.mcp;

import io.github.evyuel.confluencemcp.dto.AncestorsResponse;
import io.github.evyuel.confluencemcp.dto.ChildrenResponse;
import io.github.evyuel.confluencemcp.dto.SpacesResponse;
import io.github.evyuel.confluencemcp.service.ConfluenceNavigationService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConfluenceNavigationTools {
    private final ConfluenceNavigationService service;

    public ConfluenceNavigationTools(ConfluenceNavigationService service) {
        this.service = service;
    }

    @McpTool(name = "list_page_children", description = "List only immediate current child pages in Confluence order; does not recurse or return bodies.")
    public ChildrenResponse listPageChildren(
            @McpToolParam(description = "Confluence numeric parent page ID") String pageId,
            @McpToolParam(required = false, description = "Page size; defaults to 25 and is capped at 50") Integer limit,
            @McpToolParam(required = false, description = "Zero-based result offset; defaults to 0") Integer start) {
        return service.children(pageId, limit, start);
    }

    @McpTool(name = "get_page_ancestors", description = "Return a compact root-to-parent ancestor list for a page, excluding the target page.")
    public AncestorsResponse getPageAncestors(
            @McpToolParam(description = "Confluence numeric page ID") String pageId) {
        return service.ancestors(pageId);
    }

    @McpTool(name = "list_spaces", description = "List accessible current global Confluence Spaces, optionally filtered by exact Space key.")
    public SpacesResponse listSpaces(
            @McpToolParam(required = false, description = "Optional exact Space key") String spaceKey,
            @McpToolParam(required = false, description = "Page size; defaults to 25 and is capped at 50") Integer limit,
            @McpToolParam(required = false, description = "Zero-based result offset; defaults to 0") Integer start) {
        return service.spaces(spaceKey, limit, start);
    }
}
