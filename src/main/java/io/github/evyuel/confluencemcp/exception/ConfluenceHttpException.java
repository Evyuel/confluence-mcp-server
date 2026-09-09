package io.github.evyuel.confluencemcp.exception;

public class ConfluenceHttpException extends RuntimeException {
    private final int statusCode;
    private final boolean duplicateTitle;

    public ConfluenceHttpException(int statusCode, String message, boolean duplicateTitle) {
        super(message);
        this.statusCode = statusCode;
        this.duplicateTitle = duplicateTitle;
    }

    public int statusCode() {
        return statusCode;
    }

    public boolean isDuplicateTitle() {
        return duplicateTitle;
    }

    public boolean isNotFoundOrForbidden() {
        return statusCode == 403 || statusCode == 404;
    }
}
