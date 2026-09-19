package com.lifeadmin.extraction;

/**
 * Thrown when PDF text cannot be extracted.
 * Message is safe for internal logging; do not forward to the client verbatim.
 */
public class ExtractionException extends RuntimeException {

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
