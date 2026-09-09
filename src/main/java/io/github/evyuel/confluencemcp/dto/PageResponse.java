package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record PageResponse(String pageId, String title, String spaceKey, String url, int version,
                           String lastModified, PageFormat format, String content, int totalChars,
                           int offset, Integer nextOffset, boolean hasMore,
                           String conversionStatus, List<String> conversionWarnings) {
}
