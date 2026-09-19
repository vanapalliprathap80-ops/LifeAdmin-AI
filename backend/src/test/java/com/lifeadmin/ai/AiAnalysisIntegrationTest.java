package com.lifeadmin.ai;

import com.lifeadmin.TestPdfFactory;
import com.lifeadmin.document.DocumentType;
import com.lifeadmin.document.KeyDateRepository;
import com.lifeadmin.document.ProcessingStatus;
import com.lifeadmin.document.DocumentRepository;
import com.lifeadmin.extraction.DocumentTextRepository;
import com.lifeadmin.obligation.ObligationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Phase 4 AI pipeline.
 * Uses a real Spring context + PostgreSQL but MOCKS GeminiClient
 * to avoid real API calls and eliminate dependency on GEMINI_API_KEY.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "lifeadmin.storage.upload-dir=${java.io.tmpdir}/lifeadmin-test-ai")
class AiAnalysisIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentTextRepository documentTextRepository;
    @Autowired KeyDateRepository keyDateRepository;
    @Autowired ObligationRepository obligationRepository;
    @Autowired AIAnalysisRunRepository analysisRunRepository;

    // Mock the Gemini client so no real API calls are made
    @MockBean
    GeminiClient geminiClient;

    private static final String INSURANCE_RESPONSE = """
            {
              "documentType": "INSURANCE",
              "summary": "Home insurance policy expiring October 14 2026 with annual premium of 500 dollars.",
              "keyDates": [
                {
                  "type": "EXPIRY_DATE",
                  "date": "2026-10-14",
                  "description": "Policy expiration date",
                  "evidence": "Policy period ends on October 14 2026",
                  "confidence": 0.97
                },
                {
                  "type": "NOTICE_PERIOD_END",
                  "date": "2026-08-15",
                  "description": "Last day to give notice of non-renewal",
                  "evidence": "Written notice must be provided at least 60 days before expiry",
                  "confidence": 0.91
                }
              ],
              "obligations": [
                {
                  "type": "RENEW_POLICY",
                  "description": "Renew the insurance policy before the expiration date.",
                  "relatedDateType": "EXPIRY_DATE",
                  "evidence": "Policy period ends on October 14 2026",
                  "confidence": 0.94
                },
                {
                  "type": "GIVE_NOTICE",
                  "description": "Provide written notice of non-renewal at least 60 days before expiry.",
                  "relatedDateType": "NOTICE_PERIOD_END",
                  "evidence": "Written notice must be provided at least 60 days before expiry",
                  "confidence": 0.91
                }
              ],
              "dateRelationships": [
                {
                  "type": "NOTICE_PERIOD_BEFORE_EXPIRY",
                  "anchorDateType": "EXPIRY_DATE",
                  "offsetDays": 60,
                  "computedDate": "2026-08-15",
                  "evidence": "Written notice must be provided at least 60 days before expiry",
                  "confidence": 0.91
                }
              ],
              "uncertainties": []
            }
            """;

    @Test
    void upload_pdf_triggers_ai_analysis_and_persists_results() throws Exception {
        when(geminiClient.generate(anyString(), anyString())).thenReturn(INSURANCE_RESPONSE);

        byte[] pdf = TestPdfFactory.create(
                "Home Insurance Policy. Policy period ends on October 14 2026. " +
                "Annual premium $500. Written notice must be provided at least 60 days before expiry.");

        MockMultipartFile file = new MockMultipartFile(
                "file", "insurance.pdf", "application/pdf", pdf);

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentType").value("INSURANCE"))
                .andExpect(jsonPath("$.data.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary").isNotEmpty())
                .andReturn();

        // Extract document ID
        String body = result.getResponse().getContentAsString();
        UUID docId = UUID.fromString(extractId(body));

        // Verify KeyDates persisted
        var keyDates = keyDateRepository.findByDocumentId(docId);
        assertThat(keyDates).hasSize(2);
        assertThat(keyDates).anyMatch(kd -> kd.getDateType().name().equals("EXPIRY_DATE"));
        assertThat(keyDates).anyMatch(kd -> kd.getDateType().name().equals("NOTICE_PERIOD_END"));

        // Verify Obligations persisted
        var obligations = obligationRepository.findByDocumentId(docId);
        assertThat(obligations).hasSize(2);
        assertThat(obligations).anyMatch(ob -> "RENEW_POLICY".equals(ob.getObligationType()));

        // Verify AIAnalysisRun persisted and COMPLETED
        var runs = analysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(docId);
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getStatus()).isEqualTo(AnalysisRunStatus.COMPLETED);
        assertThat(runs.get(0).getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(runs.get(0).getRawResponse()).isNotBlank();

        // Verify DocumentText was persisted
        var docText = documentTextRepository.findByDocumentId(docId);
        assertThat(docText).isPresent();
        assertThat(docText.get().getExtractedText()).isNotBlank();

        // Cleanup
        analysisRunRepository.deleteAll(runs);
        obligationRepository.deleteAll(obligations);
        keyDateRepository.deleteAll(keyDates);
        docText.ifPresent(dt -> documentTextRepository.deleteById(dt.getId()));
        documentRepository.deleteById(docId);
    }

    @Test
    void ai_provider_failure_marks_document_needs_review_but_upload_succeeds() throws Exception {
        when(geminiClient.generate(anyString(), anyString()))
                .thenThrow(new com.lifeadmin.ai.exception.AiProviderException("Gemini unavailable"));

        byte[] pdf = TestPdfFactory.create("A simple document for testing AI failure handling.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", pdf);

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processingStatus").value("NEEDS_REVIEW"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        UUID docId = UUID.fromString(extractId(body));

        // Extracted text must still be there
        assertThat(documentTextRepository.findByDocumentId(docId)).isPresent();

        // AIAnalysisRun should be FAILED
        var runs = analysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(docId);
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getStatus()).isEqualTo(AnalysisRunStatus.FAILED);
        assertThat(runs.get(0).getErrorMessage()).isNotBlank();

        // No KeyDates or Obligations should have been persisted
        assertThat(keyDateRepository.findByDocumentId(docId)).isEmpty();
        assertThat(obligationRepository.findByDocumentId(docId)).isEmpty();

        // Cleanup
        analysisRunRepository.deleteAll(runs);
        documentTextRepository.findByDocumentId(docId).ifPresent(dt ->
                documentTextRepository.deleteById(dt.getId()));
        documentRepository.deleteById(docId);
    }

    @Test
    void reprocess_endpoint_returns_202_and_replaces_previous_results() throws Exception {
        // First upload with empty response
        when(geminiClient.generate(anyString(), anyString())).thenReturn(
                "{\"documentType\":\"UNKNOWN\",\"summary\":\"\",\"keyDates\":[],\"obligations\":[],\"dateRelationships\":[],\"uncertainties\":[]}");

        byte[] pdf = TestPdfFactory.create("Insurance policy expiring December 31 2027.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "reprocess_test.pdf", "application/pdf", pdf);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn();
        UUID docId = UUID.fromString(extractId(uploadResult.getResponse().getContentAsString()));

        // Now reprocess with a full response
        when(geminiClient.generate(anyString(), anyString())).thenReturn(INSURANCE_RESPONSE);

        mockMvc.perform(post("/api/documents/{id}/reprocess", docId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));

        // Verify new results are there (not duplicated)
        assertThat(keyDateRepository.findByDocumentId(docId)).hasSize(2);
        assertThat(obligationRepository.findByDocumentId(docId)).hasSize(2);

        // Should have 2 runs total (initial + reprocess)
        var runs = analysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(docId);
        assertThat(runs).hasSize(2);

        // Cleanup
        obligationRepository.deleteAll(obligationRepository.findByDocumentId(docId));
        keyDateRepository.deleteAll(keyDateRepository.findByDocumentId(docId));
        analysisRunRepository.deleteAll(runs);
        documentTextRepository.findByDocumentId(docId).ifPresent(dt ->
                documentTextRepository.deleteById(dt.getId()));
        documentRepository.deleteById(docId);
    }

    @Test
    void error_response_does_not_expose_api_key_or_stack_trace() throws Exception {
        when(geminiClient.generate(anyString(), anyString()))
                .thenThrow(new com.lifeadmin.ai.exception.AiProviderException("Simulated failure"));

        byte[] pdf = TestPdfFactory.create("Test document for security check.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "security_test.pdf", "application/pdf", pdf);

        // Upload succeeds — AI failure is absorbed
        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("GEMINI_API_KEY");
        assertThat(responseBody).doesNotContain("api-key");
        assertThat(responseBody).doesNotContain("at com.lifeadmin");
        assertThat(responseBody).doesNotContain("Exception");

        UUID docId = UUID.fromString(extractId(responseBody));
        // Cleanup
        analysisRunRepository.deleteAll(analysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(docId));
        documentTextRepository.findByDocumentId(docId).ifPresent(dt ->
                documentTextRepository.deleteById(dt.getId()));
        documentRepository.deleteById(docId);
    }

    private String extractId(String json) {
        int start = json.indexOf("\"id\":\"") + 6;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
