package io.github.evyuel.confluencemcp.service;

import io.github.evyuel.confluencemcp.exception.ConfluenceHttpException;
import io.github.evyuel.confluencemcp.exception.ConfluenceToolException;

final class ServiceErrors {
    private ServiceErrors() { }

    static ConfluenceToolException page(Throwable error) {
        if (error instanceof ConfluenceHttpException http && http.isNotFoundOrForbidden()) {
            return new ConfluenceToolException("Page not found or not accessible.");
        }
        if (error instanceof ConfluenceToolException tool) return tool;
        return new ConfluenceToolException("Confluence request failed.", error);
    }

    static ConfluenceToolException attachments(Throwable error) {
        if (error instanceof ConfluenceHttpException http && http.isNotFoundOrForbidden()) {
            return new ConfluenceToolException("Page or attachments not found or not accessible.");
        }
        if (error instanceof ConfluenceToolException tool) return tool;
        return new ConfluenceToolException("Confluence attachment request failed.", error);
    }
}
