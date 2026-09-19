package com.lifeadmin.document;

/**
 * Thrown when an uploaded file fails validation.
 * The message is intentionally client-safe.
 */
public class InvalidDocumentException extends RuntimeException {

    public InvalidDocumentException(String clientSafeMessage) {
        super(clientSafeMessage);
    }
}
