package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import io.github.evyuel.confluencemcp.client.ConfluenceClient;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.dto.AncestorsResponse;
import io.github.evyuel.confluencemcp.dto.ChildrenResponse;
import io.github.evyuel.confluencemcp.dto.SpacesResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ConfluenceNavigationService {
    static final int DEFAULT_LIMIT = 25;
    static final int MAX_LIMIT = 50;
    static final int MAX_ANCESTORS = 100;

    private final ConfluenceClient client;
    private final ConfluenceProperties properties;

    public ConfluenceNavigationService(ConfluenceClient client, ConfluenceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public ChildrenResponse children(String pageId, Integer limit, Integer start) {
        int actualStart = ConfluenceJson.normalizeStart(start);
        int actualLimit = ConfluenceJson.normalizeLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        try {
            JsonNode root = client.listChildren(pageId, actualStart, actualLimit);
            var pages = new ArrayList<ChildrenResponse.Page>();
            for (JsonNode page : root.path("results")) {
                pages.add(new ChildrenResponse.Page(ConfluenceJson.text(page, "/id"),
                        ConfluenceJson.text(page, "/title"), ConfluenceJson.text(page, "/status"),
                        ConfluenceJson.url(page, properties), ConfluenceJson.integer(page, "/extensions/position")));
            }
            return new ChildrenResponse(pageId, pages,
                    ConfluenceJson.pagination(root, actualStart, actualLimit, pages.size()));
        } catch (RuntimeException ex) {
            throw ServiceErrors.page(ex);
        }
    }

    public AncestorsResponse ancestors(String pageId) {
        try {
            JsonNode root = client.getPage(pageId, "ancestors,space");
            var ancestors = new ArrayList<AncestorsResponse.Ancestor>();
            int count = 0;
            for (JsonNode ancestor : root.path("ancestors")) {
                if (count++ == MAX_ANCESTORS) break;
                if (pageId.equals(ConfluenceJson.text(ancestor, "/id"))) continue;
                ancestors.add(new AncestorsResponse.Ancestor(ConfluenceJson.text(ancestor, "/id"),
                        ConfluenceJson.text(ancestor, "/title"), ConfluenceJson.text(ancestor, "/status"),
                        ConfluenceJson.url(ancestor, properties)));
            }
            return new AncestorsResponse(pageId, ConfluenceJson.text(root, "/space/key"), ancestors);
        } catch (RuntimeException ex) {
            throw ServiceErrors.page(ex);
        }
    }

    public SpacesResponse spaces(String spaceKey, Integer limit, Integer start) {
        int actualStart = ConfluenceJson.normalizeStart(start);
        int actualLimit = ConfluenceJson.normalizeLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        JsonNode root = client.listSpaces(spaceKey, actualStart, actualLimit);
        var spaces = new ArrayList<SpacesResponse.Space>();
        for (JsonNode space : root.path("results")) {
            JsonNode homepage = space.path("homepage");
            spaces.add(new SpacesResponse.Space(ConfluenceJson.text(space, "/id"),
                    ConfluenceJson.text(space, "/key"), ConfluenceJson.text(space, "/name"),
                    ConfluenceJson.text(space, "/type"), ConfluenceJson.text(space, "/status"),
                    ConfluenceJson.url(space, properties), ConfluenceJson.text(homepage, "/id"),
                    ConfluenceJson.text(homepage, "/title")));
        }
        return new SpacesResponse(spaces, ConfluenceJson.pagination(root, actualStart, actualLimit, spaces.size()));
    }
}
