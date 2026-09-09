package io.github.evyuel.confluencemcp.service;

import tools.jackson.databind.JsonNode;
import io.github.evyuel.confluencemcp.configuration.ConfluenceProperties;
import io.github.evyuel.confluencemcp.dto.Pagination;

final class ConfluenceJson {
    private ConfluenceJson() { }

    static String text(JsonNode node, String pointer) {
        JsonNode value = node == null ? null : node.at(pointer);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    static Integer integer(JsonNode node, String pointer) {
        JsonNode value = node == null ? null : node.at(pointer);
        return value == null || !value.isNumber() ? null : value.asInt();
    }

    static Long longValue(JsonNode node, String pointer) {
        JsonNode value = node == null ? null : node.at(pointer);
        return value == null || !value.isNumber() ? null : value.asLong();
    }

    static Pagination pagination(JsonNode root, int requestedStart, int requestedLimit, int size) {
        int start = root.path("start").isInt() ? root.path("start").asInt() : requestedStart;
        int limit = root.path("limit").isInt() ? root.path("limit").asInt() : requestedLimit;
        Long total = null;
        if (root.path("totalSize").isNumber()) total = root.path("totalSize").asLong();
        else if (root.path("total").isNumber()) total = root.path("total").asLong();
        boolean hasMore = text(root, "/_links/next") != null || (total != null && start + size < total);
        return new Pagination(start, limit, size, total, hasMore);
    }

    static String url(JsonNode node, ConfluenceProperties properties) {
        String webUi = text(node, "/_links/webui");
        if (webUi == null) return null;
        if (webUi.startsWith("http://") || webUi.startsWith("https://")) return webUi;
        String base = properties.baseUrl().toString().replaceAll("/$", "");
        return base + (webUi.startsWith("/") ? webUi : "/" + webUi);
    }

    static int normalizeStart(Integer start) {
        if (start == null) return 0;
        if (start < 0) throw new IllegalArgumentException("start must not be negative");
        return start;
    }

    static int normalizeLimit(Integer limit, int defaultValue, int maximum) {
        if (limit == null) return defaultValue;
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        return Math.min(limit, maximum);
    }
}
