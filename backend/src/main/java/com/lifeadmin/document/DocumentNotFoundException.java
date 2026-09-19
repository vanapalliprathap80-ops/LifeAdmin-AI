package com.lifeadmin.document;

/**
 * Thrown when a requested document does not exist in the database.
 * Message is client-safe (no IDs from internal systems).
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
