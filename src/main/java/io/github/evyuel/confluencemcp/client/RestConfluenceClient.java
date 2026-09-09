package io.github.evyuel.confluencemcp.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.exception.ConfluenceHttpException;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;

@Component
public class RestConfluenceClient implements ConfluenceClient {
    private static final int MAX_DIAGNOSTIC_LENGTH = 2_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public RestConfluenceClient(RestClient confluenceRestClient, ObjectMapper objectMapper,
                                ConfluenceProperties properties) {
        this.restClient = confluenceRestClient;
        this.objectMapper = objectMapper;
        this.token = properties.token();
    }

    @Override
    public JsonNode search(String cql, int start, int limit) {
        return get(builder -> builder.path("/rest/api/search")
                .queryParam("cql", cql).queryParam("start", start).queryParam("limit", limit).build());
    }

    @Override
    public JsonNode getPage(String pageId, String expand) {
        validatePageId(pageId);
        return get(builder -> builder.path("/rest/api/content/").pathSegment(pageId)
                .queryParam("expand", expand).build());
    }

    @Override
    public JsonNode listChildren(String pageId, int start, int limit) {
        validatePageId(pageId);
        return get(builder -> builder.path("/rest/api/content/").pathSegment(pageId).path("/child/page")
                .queryParam("status", "current").queryParam("start", start).queryParam("limit", limit).build());
    }

    @Override
    public JsonNode listSpaces(String spaceKey, int start, int limit) {
        return get(builder -> {
            builder.path("/rest/api/space").queryParam("type", "global").queryParam("status", "current")
                    .queryParam("expand", "homepage").queryParam("start", start).queryParam("limit", limit);
            if (spaceKey != null && !spaceKey.isBlank()) {
                builder.queryParam("spaceKey", spaceKey);
            }
            return builder.build();
        });
    }

    @Override
    public JsonNode listAttachments(String pageId, int start, int limit) {
        validatePageId(pageId);
        return get(builder -> builder.path("/rest/api/content/").pathSegment(pageId).path("/child/attachment")
                .queryParam("start", start).queryParam("limit", limit).build());
    }

    @Override
    public JsonNode createPage(JsonNode request) {
        try {
            return restClient.post().uri("/rest/api/content").contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(JsonNode.class);
        } catch (ResourceAccessException ex) {
            throw new ConfluenceToolException("Confluence did not return a definitive result for create_page. " +
                    "The page may have been created; verify Confluence manually before retrying.", ex);
        } catch (RestClientResponseException ex) {
            throw mapResponseException(ex);
        }
    }

    private JsonNode get(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        try {
            return restClient.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw mapResponseException(ex);
        } catch (ResourceAccessException ex) {
            throw new ConfluenceToolException("Unable to reach Confluence.", ex);
        }
    }

    private ConfluenceHttpException mapResponseException(RestClientResponseException ex) {
        String diagnostic = diagnostic(ex);
        String lower = diagnostic.toLowerCase(Locale.ROOT);
        boolean duplicate = (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 409)
                && (lower.contains("same title") || lower.contains("already exists")
                || lower.contains("duplicate") || lower.contains("title already"));
        return new ConfluenceHttpException(ex.getStatusCode().value(), diagnostic, duplicate);
    }

    private String diagnostic(RestClientResponseException ex) {
        String raw = ex.getResponseBodyAsString();
        String message = raw;
        try {
            JsonNode body = objectMapper.readTree(raw);
            message = firstText(body, "message", "errorMessage", "reason");
        } catch (Exception ignored) {
            // A short, sanitized response body is still more useful than a generic status phrase.
        }
        if (message == null || message.isBlank()) {
            message = "Confluence returned HTTP " + ex.getStatusCode().value() + ".";
        }
        message = message.replace(token, "[REDACTED]").replaceAll("(?i)Bearer\\s+[^\\s\"']+", "Bearer [REDACTED]");
        return message.substring(0, Math.min(message.length(), MAX_DIAGNOSTIC_LENGTH));
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.findValue(name);
            if (value != null && value.isValueNode()) return value.asText();
        }
        return node.toString();
    }

    private static void validatePageId(String pageId) {
        if (pageId == null || !pageId.matches("\\d+")) {
            throw new IllegalArgumentException("pageId must contain decimal digits only");
        }
    }
}
