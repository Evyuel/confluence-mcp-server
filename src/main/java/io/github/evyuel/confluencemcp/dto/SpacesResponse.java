package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record SpacesResponse(List<Space> spaces, Pagination pagination) {
    public record Space(String spaceId, String spaceKey, String name, String type, String status,
                        String url, String homepageId, String homepageTitle) { }
}
