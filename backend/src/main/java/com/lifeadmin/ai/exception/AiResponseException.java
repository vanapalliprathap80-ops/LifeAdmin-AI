package com.lifeadmin.ai.exception;

/**
 * Thrown when the LLM response cannot be parsed or fails validation
 * (malformed JSON, missing fields, invalid data types).
 */
public class AiResponseException extends RuntimeException {

    public AiResponseException(String message) {
        super(message);
    }

    public AiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
