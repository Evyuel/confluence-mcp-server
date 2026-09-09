package io.github.evyuel.confluencemcp.mcp;

import io.github.evyuel.confluencemcp.dto.SearchResponse;
import io.github.evyuel.confluencemcp.service.ConfluenceSearchService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConfluenceSearchTools {
    private final ConfluenceSearchService service;

    public ConfluenceSearchTools(ConfluenceSearchService service) {
        this.service = service;
    }

    @McpTool(name = "search_pages", description = "Search current Confluence pages by page text and return compact metadata and excerpts, never full page bodies.")
    public SearchResponse searchPages(
            @McpToolParam(description = "Required full-text query") String query,
            @McpToolParam(required = false, description = "Optional exact Space key filter") String spaceKey,
            @McpToolParam(required = false, description = "Page size; defaults to 10 and is capped at 20") Integer limit,
            @McpToolParam(required = false, description = "Zero-based result offset; defaults to 0") Integer start) {
        return service.search(query, spaceKey, limit, start);
    }
}
