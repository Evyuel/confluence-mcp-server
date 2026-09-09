package io.github.evyuel.confluencemcp.client;

import tools.jackson.databind.JsonNode;

public interface ConfluenceClient {
    JsonNode search(String cql, int start, int limit);
    JsonNode getPage(String pageId, String expand);
    JsonNode listChildren(String pageId, int start, int limit);
    JsonNode listSpaces(String spaceKey, int start, int limit);
    JsonNode listAttachments(String pageId, int start, int limit);
    JsonNode createPage(JsonNode request);
}
