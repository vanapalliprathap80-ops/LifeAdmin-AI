package com.lifeadmin.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.exception.AiResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Parses the raw LLM text output into a structured AiAnalysisResponse.
 * Handles JSON wrapped in markdown code fences.
 */
@Component
public class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses raw LLM text into AiAnalysisResponse.
     *
     * @param rawText the raw text output from the LLM
     * @return parsed response DTO
     * @throws AiResponseException if the text cannot be parsed as valid JSON
     */
    public AiAnalysisResponse parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new AiResponseException("AI returned an empty response");
        }

        String json = stripCodeFences(rawText.trim());

        try {
            AiAnalysisResponse response = objectMapper.readValue(json, AiAnalysisResponse.class);
            log.debug("Successfully parsed AI response");
            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new AiResponseException("AI response is not valid JSON: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * Strips markdown code fences that LLMs sometimes wrap around JSON.
     * Handles: ```json ... ``` and ``` ... ```
     */
    String stripCodeFences(String text) {
        if (text.startsWith("```")) {
            // Remove opening fence (possibly with language tag)
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            // Remove closing fence
            int lastFence = text.lastIndexOf("```");
            if (lastFence > 0) {
                text = text.substring(0, lastFence);
            }
            return text.trim();
        }
        return text;
    }
}
