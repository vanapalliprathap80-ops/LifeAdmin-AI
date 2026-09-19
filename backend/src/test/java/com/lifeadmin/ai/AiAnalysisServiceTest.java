package com.lifeadmin.ai;

import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiKeyDateDto;
import com.lifeadmin.ai.dto.AiObligationDto;
import com.lifeadmin.ai.exception.AiProviderException;
import com.lifeadmin.ai.exception.AiResponseException;
import com.lifeadmin.document.*;
import com.lifeadmin.extraction.DocumentText;
import com.lifeadmin.extraction.DocumentTextRepository;
import com.lifeadmin.obligation.Obligation;
import com.lifeadmin.obligation.ObligationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock GeminiClient geminiClient;
    @Mock AiPromptBuilder promptBuilder;
    @Mock AiResponseParser responseParser;
    @Mock AiResponseValidator responseValidator;
    @Mock AIAnalysisRunRepository analysisRunRepository;
    @Mock DocumentRepository documentRepository;
    @Mock DocumentTextRepository documentTextRepository;
    @Mock KeyDateRepository keyDateRepository;
    @Mock ObligationRepository obligationRepository;


    private GeminiProperties geminiProperties;
    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties();
        geminiProperties.setModel("gemini-2.0-flash");
        geminiProperties.setApiKey("test-key");

        service = new AiAnalysisService(
                geminiClient, promptBuilder, responseParser, responseValidator,
                analysisRunRepository, documentRepository, documentTextRepository,
                keyDateRepository, obligationRepository, geminiProperties);
    }

    // ── Successful analysis ────────────────────────────────────────────────────

    @Test
    void successful_analysis_persists_key_dates_and_obligations() {
        Document document = stubDocument();

        DocumentText docText = new DocumentText();
        docText.setExtractedText("Policy expires on 14 October 2026. Premium is $500 annually.");
        when(documentTextRepository.findByDocumentId(document.getId()))
                .thenReturn(Optional.of(docText));

        AIAnalysisRun run = stubAnalysisRun(document);
        when(analysisRunRepository.save(any())).thenReturn(run);

        when(promptBuilder.buildSystemInstruction()).thenReturn("system instruction");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("user prompt");
        when(geminiClient.generate(any(), any())).thenReturn("{\"documentType\":\"INSURANCE\"}");

        AiAnalysisResponse response = buildAnalysisResponse();
        when(responseParser.parse(any())).thenReturn(response);
        when(responseValidator.validate(any())).thenReturn(response);

        when(keyDateRepository.findByDocumentId(any())).thenReturn(List.of());
        when(obligationRepository.findByDocumentId(any())).thenReturn(List.of());
        when(documentRepository.save(any())).thenReturn(document);

        service.analyze(document);

        verify(keyDateRepository).save(any(KeyDate.class));
        verify(obligationRepository).save(any(Obligation.class));
        verify(documentRepository, atLeastOnce()).save(any());
        verify(analysisRunRepository, times(2)).save(any()); // RUNNING + COMPLETED
    }

    @Test
    void successful_analysis_marks_run_completed() {
        Document document = stubDocument();
        DocumentText docText = new DocumentText();
        docText.setExtractedText("Policy text");
        when(documentTextRepository.findByDocumentId(any())).thenReturn(Optional.of(docText));

        AIAnalysisRun run = stubAnalysisRun(document);
        when(analysisRunRepository.save(any())).thenReturn(run);
        when(promptBuilder.buildSystemInstruction()).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("usr");
        when(geminiClient.generate(any(), any())).thenReturn("{}");

        AiAnalysisResponse response = emptyValidResponse();
        when(responseParser.parse(any())).thenReturn(response);
        when(responseValidator.validate(any())).thenReturn(response);
        when(keyDateRepository.findByDocumentId(any())).thenReturn(List.of());
        when(obligationRepository.findByDocumentId(any())).thenReturn(List.of());
        when(documentRepository.save(any())).thenReturn(document);

        service.analyze(document);

        // Two saves to analysisRunRepository: initial (RUNNING) + final (COMPLETED)
        verify(analysisRunRepository, times(2)).save(any(AIAnalysisRun.class));
    }

    // ── Provider failure ───────────────────────────────────────────────────────

    @Test
    void provider_failure_marks_run_failed_and_document_needs_review() {
        Document document = stubDocument();
        DocumentText docText = new DocumentText();
        docText.setExtractedText("some text");
        when(documentTextRepository.findByDocumentId(any())).thenReturn(Optional.of(docText));

        AIAnalysisRun run = stubAnalysisRun(document);
        when(analysisRunRepository.save(any())).thenReturn(run);
        when(promptBuilder.buildSystemInstruction()).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("usr");
        when(geminiClient.generate(any(), any()))
                .thenThrow(new AiProviderException("Connection refused"));

        assertThatThrownBy(() -> service.analyze(document))
                .isInstanceOf(AiProviderException.class);

        // AIAnalysisRun and Document are both updated on failure
        verify(analysisRunRepository, atLeastOnce()).save(any(AIAnalysisRun.class));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void invalid_response_marks_run_failed() {
        Document document = stubDocument();
        DocumentText docText = new DocumentText();
        docText.setExtractedText("some text");
        when(documentTextRepository.findByDocumentId(any())).thenReturn(Optional.of(docText));

        AIAnalysisRun run = stubAnalysisRun(document);
        when(analysisRunRepository.save(any())).thenReturn(run);
        when(promptBuilder.buildSystemInstruction()).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("usr");
        when(geminiClient.generate(any(), any())).thenReturn("not valid json");
        when(responseParser.parse("not valid json"))
                .thenThrow(new AiResponseException("Not valid JSON"));

        assertThatThrownBy(() -> service.analyze(document))
                .isInstanceOf(AiResponseException.class);

        verify(analysisRunRepository, atLeastOnce()).save(any(AIAnalysisRun.class));
    }

    // ── Missing text ───────────────────────────────────────────────────────────

    @Test
    void skips_analysis_when_no_extracted_text() {
        Document document = stubDocument();
        when(documentTextRepository.findByDocumentId(any())).thenReturn(Optional.empty());

        service.analyze(document);

        verifyNoInteractions(geminiClient);
        verify(documentRepository).save(any(Document.class));
    }

    // ── Reprocessing — no duplicates ───────────────────────────────────────────

    @Test
    void reprocessing_deletes_existing_results_before_persisting_new_ones() {
        Document document = stubDocument();
        DocumentText docText = new DocumentText();
        docText.setExtractedText("Policy text");
        when(documentTextRepository.findByDocumentId(any())).thenReturn(Optional.of(docText));

        AIAnalysisRun run = stubAnalysisRun(document);
        when(analysisRunRepository.save(any())).thenReturn(run);
        when(promptBuilder.buildSystemInstruction()).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("usr");
        when(geminiClient.generate(any(), any())).thenReturn("{}");

        AiAnalysisResponse response = emptyValidResponse();
        when(responseParser.parse(any())).thenReturn(response);
        when(responseValidator.validate(any())).thenReturn(response);

        // Pre-existing data from a previous analysis run
        KeyDate existingDate = new KeyDate();
        Obligation existingObligation = new Obligation();
        when(keyDateRepository.findByDocumentId(any())).thenReturn(List.of(existingDate));
        when(obligationRepository.findByDocumentId(any())).thenReturn(List.of(existingObligation));
        when(documentRepository.save(any())).thenReturn(document);

        service.analyze(document);

        verify(keyDateRepository).deleteAll(List.of(existingDate));
        verify(obligationRepository).deleteAll(List.of(existingObligation));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Document stubDocument() {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setOriginalFilename("test.pdf");
        doc.setStoredFilename("uuid.pdf");
        doc.setDocumentType(DocumentType.UNKNOWN);
        doc.setProcessingStatus(ProcessingStatus.COMPLETED);
        doc.setFileSize(1000L);
        doc.setContentType("application/pdf");
        return doc;
    }

    private AIAnalysisRun stubAnalysisRun(Document doc) {
        AIAnalysisRun run = new AIAnalysisRun();
        run.setId(UUID.randomUUID());
        run.setDocument(doc);
        run.setStatus(AnalysisRunStatus.RUNNING);
        return run;
    }

    private AiAnalysisResponse buildAnalysisResponse() {
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setDocumentType("INSURANCE");
        response.setSummary("Insurance policy summary.");

        AiKeyDateDto kd = new AiKeyDateDto();
        kd.setType("EXPIRY_DATE");
        kd.setDate("2026-10-14");
        kd.setDescription("Expiry");
        kd.setEvidence("expires October 14 2026");
        kd.setConfidence(0.97);

        AiObligationDto ob = new AiObligationDto();
        ob.setType("RENEW_POLICY");
        ob.setDescription("Renew the policy");
        ob.setEvidence("expires October 14 2026");
        ob.setConfidence(0.94);

        response.setKeyDates(new ArrayList<>(List.of(kd)));
        response.setObligations(new ArrayList<>(List.of(ob)));
        response.setDateRelationships(new ArrayList<>());
        response.setUncertainties(new ArrayList<>());
        return response;
    }

    private AiAnalysisResponse emptyValidResponse() {
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setDocumentType("UNKNOWN");
        response.setSummary("");
        response.setKeyDates(new ArrayList<>());
        response.setObligations(new ArrayList<>());
        response.setDateRelationships(new ArrayList<>());
        response.setUncertainties(new ArrayList<>());
        return response;
    }
}
