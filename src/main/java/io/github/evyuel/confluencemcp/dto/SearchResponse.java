package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record SearchResponse(List<Page> pages, Pagination pagination) {
    public record Page(String pageId, String title, String spaceKey, String spaceName,
                       String excerpt, String url, String lastModified) { }
}
