package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record ChildrenResponse(String parentPageId, List<Page> pages, Pagination pagination) {
    public record Page(String pageId, String title, String status, String url, Integer position) { }
}
