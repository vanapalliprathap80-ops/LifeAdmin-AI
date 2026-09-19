package com.lifeadmin.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.exception.AiResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
    }

    // ── Valid JSON ─────────────────────────────────────────────────────────────

    @Test
    void parses_valid_json_response() {
        String json = """
                {
                  "documentType": "INSURANCE",
                  "summary": "A home insurance policy expiring 2026-10-14.",
                  "keyDates": [
                    {
                      "type": "EXPIRY_DATE",
                      "date": "2026-10-14",
                      "description": "Policy expiry",
                      "evidence": "Policy expires on 14 October 2026",
                      "confidence": 0.97
                    }
                  ],
                  "obligations": [
                    {
                      "type": "RENEW_POLICY",
                      "description": "Renew the insurance policy before expiry",
                      "evidence": "Policy expires on 14 October 2026",
                      "confidence": 0.94
                    }
                  ],
                  "dateRelationships": [],
                  "uncertainties": []
                }
                """;

        AiAnalysisResponse result = parser.parse(json);

        assertThat(result.getDocumentType()).isEqualTo("INSURANCE");
        assertThat(result.getSummary()).contains("insurance policy");
        assertThat(result.getKeyDates()).hasSize(1);
        assertThat(result.getKeyDates().get(0).getDate()).isEqualTo("2026-10-14");
        assertThat(result.getObligations()).hasSize(1);
        assertThat(result.getObligations().get(0).getType()).isEqualTo("RENEW_POLICY");
    }

    @Test
    void parses_json_wrapped_in_json_code_fence() {
        String json = """
                ```json
                {"documentType":"LEASE","summary":"A lease.","keyDates":[],"obligations":[],"dateRelationships":[],"uncertainties":[]}
                ```
                """;

        AiAnalysisResponse result = parser.parse(json);
        assertThat(result.getDocumentType()).isEqualTo("LEASE");
    }

    @Test
    void parses_json_wrapped_in_plain_code_fence() {
        String json = "```\n{\"documentType\":\"BILL\",\"summary\":\"A bill.\",\"keyDates\":[],\"obligations\":[],\"dateRelationships\":[],\"uncertainties\":[]}\n```";

        AiAnalysisResponse result = parser.parse(json);
        assertThat(result.getDocumentType()).isEqualTo("BILL");
    }

    @Test
    void ignores_unknown_fields() {
        String json = """
                {
                  "documentType": "UNKNOWN",
                  "summary": "Test",
                  "keyDates": [],
                  "obligations": [],
                  "dateRelationships": [],
                  "uncertainties": [],
                  "unexpectedField": "should be ignored",
                  "anotherUnknown": 42
                }
                """;

        assertThatNoException().isThrownBy(() -> parser.parse(json));
    }

    // ── Malformed JSON ─────────────────────────────────────────────────────────

    @Test
    void throws_on_malformed_json() {
        assertThatThrownBy(() -> parser.parse("{not valid json"))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("valid JSON");
    }

    @Test
    void throws_on_empty_input() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(AiResponseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throws_on_null_input() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(AiResponseException.class);
    }

    @Test
    void throws_on_incomplete_json() {
        assertThatThrownBy(() -> parser.parse("{\"documentType\": \"INSURANCE\""))
                .isInstanceOf(AiResponseException.class);
    }

    @Test
    void throws_on_json_array_instead_of_object() {
        assertThatThrownBy(() -> parser.parse("[\"not\", \"an\", \"object\"]"))
                .isInstanceOf(AiResponseException.class);
    }

    // ── Code fence stripping ───────────────────────────────────────────────────

    @Test
    void strip_code_fences_with_language_tag() {
        String input = "```json\n{\"a\":1}\n```";
        assertThat(parser.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void strip_plain_code_fences() {
        String input = "```\n{\"a\":1}\n```";
        assertThat(parser.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void does_not_strip_when_no_fences() {
        String input = "{\"a\":1}";
        assertThat(parser.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }
}
