package com.lifeadmin.ai;

import com.lifeadmin.ai.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Gemini-specific HTTP client. Isolated from the rest of the application
 * so no other service depends on Gemini API details.
 *
 * Uses Spring's RestClient (synchronous, available in Boot 3.2+).
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .build();
    }

    /**
     * Sends a prompt to Gemini and returns the raw text response.
     *
     * @param systemInstruction the system-level instruction
     * @param userPrompt the user-level prompt with document text
     * @return raw text content from Gemini's response
     * @throws AiProviderException on any provider-level failure
     */
    public String generate(String systemInstruction, String userPrompt) {
        return generate(systemInstruction, userPrompt, null);
    }

    /**
     * Sends a prompt to Gemini with an enforced JSON response schema.
     *
     * @param systemInstruction the system-level instruction
     * @param userPrompt the user-level prompt
     * @param responseSchema optional JSON schema for structured outputs
     * @return raw JSON string matching the schema
     */
    public String generate(String systemInstruction, String userPrompt, JsonNode responseSchema) {
        validateConfiguration();

        String requestBody = buildRequestBody(systemInstruction, userPrompt, responseSchema);
        String url = "/" + properties.getModel() + ":generateContent?key=" + properties.getApiKey();

        log.info("Sending analysis request to Gemini model={}", properties.getModel());

        int maxRetries = 3;
        int delayMs = 1500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                            String body;
                            try {
                                body = new String(response.getBody().readAllBytes());
                            } catch (Exception ex) {
                                body = "(unreadable)";
                            }
                            log.error("Gemini API client error: status={}, body={}", response.getStatusCode(), body);
                            if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
                                throw new AiProviderException("Gemini API authentication failed. Check GEMINI_API_KEY.");
                            }
                            if (response.getStatusCode().value() == 429) {
                                throw new AiProviderException("Gemini API rate limit exceeded (429)");
                            }
                            throw new AiProviderException("Gemini API error: " + response.getStatusCode());
                        })
                        .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                            log.warn("Gemini API server error: status={}", response.getStatusCode());
                            throw new AiProviderException("Gemini API server error: " + response.getStatusCode());
                        })
                        .body(String.class);

                return extractTextFromResponse(responseBody);

            } catch (AiProviderException e) {
                boolean isTransient = e.getMessage() != null && (e.getMessage().contains("503") || e.getMessage().contains("429"));
                if (isTransient && attempt < maxRetries) {
                    log.warn("Gemini API transient failure (attempt {}/{}): {}. Retrying in {}ms...",
                            attempt, maxRetries, e.getMessage(), delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    delayMs *= 2;
                    continue;
                }
                throw e;
            } catch (ResourceAccessException e) {
                if (attempt < maxRetries) {
                    log.warn("Gemini connection timeout (attempt {}/{}). Retrying...", attempt, maxRetries);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                log.error("Gemini API timeout or connection error: {}", e.getMessage());
                throw new AiProviderException("Gemini API timeout or connection error", e);
            } catch (Exception e) {
                log.error("Unexpected error calling Gemini API: {}", e.getMessage());
                throw new AiProviderException("Unexpected error calling Gemini API", e);
            }
        }
        throw new AiProviderException("Gemini API failed after " + maxRetries + " attempts");
    }

    private void validateConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderException("GEMINI_API_KEY is not configured");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new AiProviderException("GEMINI_MODEL is not configured");
        }
    }

    /**
     * Builds the Gemini API request body JSON.
     */
    private String buildRequestBody(String systemInstruction, String userPrompt, JsonNode responseSchema) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            // System instruction
            ObjectNode systemInstructionNode = objectMapper.createObjectNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", systemInstruction);
            ArrayNode systemParts = objectMapper.createArrayNode().add(systemPart);
            systemInstructionNode.set("parts", systemParts);
            root.set("system_instruction", systemInstructionNode);

            // User content
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", userPrompt);
            ArrayNode userParts = objectMapper.createArrayNode().add(userPart);
            userContent.set("parts", userParts);
            root.set("contents", objectMapper.createArrayNode().add(userContent));

            // Generation config
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 4096);
            if (responseSchema != null) {
                generationConfig.put("responseMimeType", "application/json");
                generationConfig.set("responseSchema", responseSchema);
            }
            root.set("generationConfig", generationConfig);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new AiProviderException("Failed to build Gemini request body", e);
        }
    }

    /**
     * Extracts the text content from Gemini's response JSON.
     * Gemini returns: { candidates: [{ content: { parts: [{ text: "..." }] } }] }
     */
    private String extractTextFromResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AiProviderException("Gemini returned an empty response");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("Unknown Gemini error");
                throw new AiProviderException("Gemini API error: " + errorMsg);
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiProviderException("Gemini returned no candidates in response");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new AiProviderException("Gemini response has no content parts");
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                throw new AiProviderException("Gemini returned empty text content");
            }

            log.info("Received Gemini response: {} characters", text.length());
            return text;

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse Gemini response", e);
        }
    }
}
