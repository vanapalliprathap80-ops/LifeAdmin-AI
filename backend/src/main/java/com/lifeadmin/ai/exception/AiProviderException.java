package com.lifeadmin.ai.exception;

/**
 * Thrown when the LLM provider (Gemini) returns an error,
 * times out, or is unreachable.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
