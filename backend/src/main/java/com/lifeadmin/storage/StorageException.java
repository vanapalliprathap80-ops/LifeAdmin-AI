package com.lifeadmin.storage;

/**
 * Thrown when a file cannot be safely stored.
 * Message is safe for logging but must NOT be forwarded to the client verbatim.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
