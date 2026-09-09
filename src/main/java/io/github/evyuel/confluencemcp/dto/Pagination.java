package io.github.evyuel.confluencemcp.dto;

public record Pagination(int start, int limit, int size, Long totalCount, boolean hasMore) {
}
