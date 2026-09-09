package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.dto.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ConfluenceSearchService {
    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 20;

    private final ConfluenceClient client;
    private final ConfluenceProperties properties;

    public ConfluenceSearchService(ConfluenceClient client, ConfluenceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public SearchResponse search(String query, String spaceKey, Integer limit, Integer start) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("query must not be blank");
        int actualStart = ConfluenceJson.normalizeStart(start);
        int actualLimit = ConfluenceJson.normalizeLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        String cql = "type=page AND text ~ \"" + escapeCqlLiteral(query) + "\"";
        if (spaceKey != null && !spaceKey.isBlank()) {
            cql += " AND space=\"" + escapeCqlLiteral(spaceKey) + "\"";
        }

        JsonNode root = client.search(cql, actualStart, actualLimit);
        var pages = new ArrayList<SearchResponse.Page>();
        for (JsonNode result : root.path("results")) {
            JsonNode content = result.path("content");
            JsonNode space = content.path("space");
            pages.add(new SearchResponse.Page(
                    ConfluenceJson.text(content, "/id"), ConfluenceJson.text(content, "/title"),
                    ConfluenceJson.text(space, "/key"), ConfluenceJson.text(space, "/name"),
                    ConfluenceJson.text(result, "/excerpt"), ConfluenceJson.url(content, properties),
                    firstNonNull(ConfluenceJson.text(result, "/lastModified"),
                            ConfluenceJson.text(content, "/history/lastUpdated/when"))));
        }
        return new SearchResponse(pages, ConfluenceJson.pagination(root, actualStart, actualLimit, pages.size()));
    }

    static String escapeCqlLiteral(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }
}
