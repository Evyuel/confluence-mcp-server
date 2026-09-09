package io.github.evyuel.confluencemcp.dto;

import java.util.List;

public record AttachmentsResponse(String pageId, List<Attachment> attachments, Pagination pagination) {
    public record Attachment(String attachmentId, String fileName, String mediaType, Long fileSize,
                             Integer version, String downloadUrl, String comment,
                             String createdDate, String lastModified) { }
}
