package com.lifeadmin.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeadmin.ai.GeminiClient;
import com.lifeadmin.document.Document;
import com.lifeadmin.document.DocumentRepository;
import com.lifeadmin.document.DocumentUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.lifeadmin.extraction.PdfExtractionService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DocumentUploadService uploadService;
    @Autowired ActionRepository actionRepository;
    @Autowired DocumentRepository documentRepository;
    @Autowired ObjectMapper objectMapper;

    @MockBean GeminiClient geminiClient;
    @MockBean Clock clock;
    @MockBean PdfExtractionService pdfExtractionService;

    private static final String MOCK_AI_RESPONSE = """
    ```json
    {
      "documentType": "LEASE",
      "summary": "Mock lease agreement",
      "keyDates": [
        {
          "type": "EXPIRY_DATE",
          "date": "2026-12-31",
          "description": "Lease expiry",
          "confidence": 0.99
        }
      ],
      "obligations": [
        {
          "type": "PROVIDE_NOTICE",
          "description": "Provide written notice before vacating",
          "relatedDateType": "NOTICE_PERIOD_BEFORE_EXPIRY",
          "confidence": 0.95
        }
      ],
      "dateRelationships": [
        {
          "type": "NOTICE_PERIOD_BEFORE_EXPIRY",
          "anchorDateType": "EXPIRY_DATE",
          "offsetDays": 60,
          "computedDate": "2026-11-01",
          "confidence": 0.9
        }
      ]
    }
    ```
    """;

    @BeforeEach
    void setUp() {
        actionRepository.deleteAll();
        documentRepository.deleteAll();

        // Fix clock to 2026-09-18 so we can deterministically test priority
        // Deadline is 2026-11-01. From 09-18 to 11-01 is 44 days (=> MEDIUM priority)
        when(clock.instant()).thenReturn(Instant.parse("2026-09-18T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void testEndToEndActionGenerationAndApi() throws Exception {
        // 1. Mock the AI response and PDF extraction
        when(geminiClient.generate(anyString(), anyString())).thenReturn(MOCK_AI_RESPONSE);
        when(pdfExtractionService.extract(any(byte[].class))).thenReturn("Dummy extracted text");
        when(pdfExtractionService.hasExtractableText(anyString())).thenReturn(true);
        // validatePdf is void, so nothing to mock there (it will do nothing by default)

        // 2. Upload a document (which triggers extraction -> AI -> Action Generation)
        MockMultipartFile file = new MockMultipartFile(
                "file", "lease.pdf", "application/pdf", "%PDF-1.4 dummy pdf content".getBytes());
        Document document = uploadService.upload(file);

        // 3. Verify actions were created in DB
        List<Action> actions = actionRepository.findByDocumentId(document.getId());
        assertThat(actions).hasSize(1);
        
        Action action = actions.get(0);
        assertThat(action.getTitle()).isEqualTo("Provide notice");
        assertThat(action.getDeadline()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(action.getPriority()).isEqualTo(ActionPriority.MEDIUM);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING);

        // 4. Test GET /api/actions
        mockMvc.perform(get("/api/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Provide notice"))
                .andExpect(jsonPath("$.data[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data[0].deadline").value("2026-11-01"));

        // 5. Test PATCH /api/actions/{id}/complete
        mockMvc.perform(patch("/api/actions/" + action.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 6. Verify Reprocessing preserves completed action
        uploadService.reprocess(document.getId());
        
        List<Action> afterReprocess = actionRepository.findByDocumentId(document.getId());
        assertThat(afterReprocess).hasSize(1); // Deduplication prevented a second action
        assertThat(afterReprocess.get(0).getStatus()).isEqualTo(ActionStatus.COMPLETED); // Still completed
    }
}
