package com.lifeadmin.action;

/**
 * Thrown when a requested action does not exist in the database.
 * Message is client-safe (no internal IDs exposed).
 */
public class ActionNotFoundException extends RuntimeException {

    public ActionNotFoundException(String message) {
        super(message);
    }
}
