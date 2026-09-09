package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record AncestorsResponse(String pageId, String spaceKey, List<Ancestor> ancestors) {
    public record Ancestor(String pageId, String title, String status, String url) { }
}
