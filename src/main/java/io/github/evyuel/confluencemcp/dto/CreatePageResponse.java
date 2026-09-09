package io.github.evyuel.confluencemcp.dto;

public record CreatePageResponse(String pageId, String requestedTitle, String actualTitle,
                                 boolean renamedDueToConflict, String url) {
}
