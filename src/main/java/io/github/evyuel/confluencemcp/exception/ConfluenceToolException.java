package io.github.evyuel.confluencemcp.exception;

public class ConfluenceToolException extends RuntimeException {
    public ConfluenceToolException(String message) {
        super(message);
    }

    public ConfluenceToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
