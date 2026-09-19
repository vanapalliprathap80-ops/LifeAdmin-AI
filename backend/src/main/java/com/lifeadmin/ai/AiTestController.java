package com.lifeadmin.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test/gemini")
public class AiTestController {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public AiTestController(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> testGeminiConnection() {
        try {
            // Build a simple JSON Schema for the expected response
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "OBJECT");
            ObjectNode properties = objectMapper.createObjectNode();
            
            ObjectNode statusNode = objectMapper.createObjectNode();
            statusNode.put("type", "STRING");
            statusNode.put("description", "Should be 'OK' if you understand this prompt.");
            
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("type", "STRING");
            messageNode.put("description", "A short greeting message.");
            
            properties.set("status", statusNode);
            properties.set("message", messageNode);
            schema.set("properties", properties);

            String systemPrompt = "You are a test endpoint. Respond exactly as the schema requires.";
            String userPrompt = "Say hello and confirm your status.";

            String rawJsonResponse = geminiClient.generate(systemPrompt, userPrompt, schema);
            
            // The response should already be structured JSON due to responseSchema
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "rawResponse", objectMapper.readTree(rawJsonResponse)
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
