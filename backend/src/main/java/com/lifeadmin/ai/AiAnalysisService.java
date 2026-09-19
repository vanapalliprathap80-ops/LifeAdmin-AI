package com.lifeadmin.ai;

import com.lifeadmin.action.ActionGenerationService;
import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiKeyDateDto;
import com.lifeadmin.ai.dto.AiObligationDto;
import com.lifeadmin.ai.exception.AiProviderException;
import com.lifeadmin.ai.exception.AiResponseException;
import com.lifeadmin.document.Document;
import com.lifeadmin.document.DocumentRepository;
import com.lifeadmin.document.DocumentType;
import com.lifeadmin.document.KeyDate;
import com.lifeadmin.document.KeyDateRepository;
import com.lifeadmin.document.KeyDateType;
import com.lifeadmin.document.ProcessingStatus;
import com.lifeadmin.extraction.DocumentText;
import com.lifeadmin.extraction.DocumentTextRepository;
import com.lifeadmin.obligation.Obligation;
import com.lifeadmin.obligation.ObligationRepository;
import com.lifeadmin.obligation.ObligationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the AI analysis pipeline:
 *
 *  DocumentText → Prompt → GeminiClient → Parser → Validator → Persistence
 *
 * Key safety rules:
 * - A new AIAnalysisRun is created BEFORE calling Gemini.
 * - The analysis runs in its own transaction scope (REQUIRES_NEW).
 *   Failure here does NOT roll back the upload transaction.
 * - Previous KeyDates and Obligations are deleted before persisting new ones (safe reprocess).
 * - LLM is NOT trusted for date arithmetic; computedDate fields are stored as-is for Phase 5.
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final GeminiClient geminiClient;
    private final AiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final AiResponseValidator responseValidator;
    private final AIAnalysisRunRepository analysisRunRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final KeyDateRepository keyDateRepository;
    private final ObligationRepository obligationRepository;
    private final GeminiProperties geminiProperties;


    public AiAnalysisService(
            GeminiClient geminiClient,
            AiPromptBuilder promptBuilder,
            AiResponseParser responseParser,
            AiResponseValidator responseValidator,
            AIAnalysisRunRepository analysisRunRepository,
            DocumentRepository documentRepository,
            DocumentTextRepository documentTextRepository,
            KeyDateRepository keyDateRepository,
            ObligationRepository obligationRepository,
            GeminiProperties geminiProperties) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.responseValidator = responseValidator;
        this.analysisRunRepository = analysisRunRepository;
        this.documentRepository = documentRepository;
        this.documentTextRepository = documentTextRepository;
        this.keyDateRepository = keyDateRepository;
        this.obligationRepository = obligationRepository;
        this.geminiProperties = geminiProperties;
    }

    /**
     * Runs AI analysis for a document. Uses REQUIRES_NEW so that a failure
     * here does not roll back the calling upload transaction.
     *
     * @param document the document whose text should be analysed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {AiProviderException.class, AiResponseException.class})
    public java.util.Optional<AiAnalysisResponse> analyze(Document document) {
        UUID documentId = document.getId();
        log.info("Starting AI analysis for document {}", documentId);

        // Step 1 — Verify extracted text exists
        Optional<DocumentText> textOpt = documentTextRepository.findByDocumentId(documentId);
        if (textOpt.isEmpty()) {
            log.error("No extracted text found for document {}, skipping AI analysis", documentId);
            markDocumentCompleted(document);
            return java.util.Optional.empty();
        }
        String extractedText = textOpt.get().getExtractedText();

        // Step 2 — Create AIAnalysisRun record (RUNNING)
        AIAnalysisRun run = new AIAnalysisRun();
        run.setDocument(document);
        run.setUserId(document.getUserId());
        run.setProvider("gemini");
        run.setModel(geminiProperties.getModel());
        run.setStatus(AnalysisRunStatus.RUNNING);
        run = analysisRunRepository.save(run);
        UUID runId = run.getId();
        log.info("Created AIAnalysisRun {} for document {}", runId, documentId);

        String rawResponse = null;
        try {
            // Step 3 — Build prompt
            String systemInstruction = promptBuilder.buildSystemInstruction();
            String userPrompt = promptBuilder.buildUserPrompt(extractedText);

            // Step 4 — Call Gemini
            rawResponse = geminiClient.generate(systemInstruction, userPrompt, promptBuilder.getAnalysisSchema());

            // Step 5 — Parse JSON response
            AiAnalysisResponse parsed = responseParser.parse(rawResponse);

            // Step 6 — Validate response
            AiAnalysisResponse validated = responseValidator.validate(parsed);

            // Step 7 — Delete previous analysis results (safe reprocessing)
            deleteExistingAnalysisResults(documentId);

            // Step 8 — Persist KeyDates
            List<KeyDate> savedKeyDates = persistKeyDates(document, validated.getKeyDates());

            // Step 9 — Persist Obligations
            List<Obligation> savedObligations = persistObligations(document, validated.getObligations());
            

            // Step 10 — Update Document: type + summary
            DocumentType docType = DocumentType.valueOf(validated.getDocumentType());
            document.setDocumentType(docType);
            if (validated.getSummary() != null && !validated.getSummary().isBlank()) {
                document.setSummary(validated.getSummary());
            }
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            documentRepository.save(document);

            // Step 11 — Mark run COMPLETED
            run.setStatus(AnalysisRunStatus.COMPLETED);
            run.setRawResponse(rawResponse);
            run.setCompletedAt(Instant.now());
            analysisRunRepository.save(run);

            log.info("AI analysis COMPLETED for document {}: type={}, keyDates={}, obligations={}",
                    documentId, docType,
                    validated.getKeyDates().size(),
                    validated.getObligations().size());

            return java.util.Optional.of(validated);

        } catch (AiProviderException | AiResponseException e) {
            log.error("AI analysis FAILED for document {}: {}", documentId, e.getMessage());
            failRun(run, rawResponse, e.getMessage());
            markDocumentCompleted(document);
            throw e; 
        } catch (Exception e) {
            log.error("Unexpected error during AI analysis for document {}: {}", documentId, e.getMessage(), e);
            failRun(run, rawResponse, "Unexpected error: " + e.getMessage());
            markDocumentCompleted(document);
            throw new AiProviderException("Unexpected error during AI analysis", e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Deletes all existing KeyDates and Obligations for a document before
     * persisting new ones. Ensures reprocessing never creates duplicates.
     */
    private void deleteExistingAnalysisResults(UUID documentId) {
        List<KeyDate> existingDates = keyDateRepository.findByDocumentId(documentId);
        if (!existingDates.isEmpty()) {
            keyDateRepository.deleteAll(existingDates);
            log.info("Deleted {} existing KeyDate records for document {}", existingDates.size(), documentId);
        }

        List<Obligation> existingObligations = obligationRepository.findByDocumentId(documentId);
        if (!existingObligations.isEmpty()) {
            obligationRepository.deleteAll(existingObligations);
            log.info("Deleted {} existing Obligation records for document {}", existingObligations.size(), documentId);
        }
    }

    private List<KeyDate> persistKeyDates(Document document, List<AiKeyDateDto> keyDateDtos) {
        List<KeyDate> savedDates = new java.util.ArrayList<>();
        for (AiKeyDateDto dto : keyDateDtos) {
            try {
                KeyDate kd = new KeyDate();
                kd.setDocument(document);
                kd.setDateValue(LocalDate.parse(dto.getDate()));
                kd.setDateType(KeyDateType.valueOf(dto.getType()));
                kd.setDescription(dto.getDescription());
                kd.setEvidence(dto.getEvidence());
                if (dto.getConfidence() != null) {
                    kd.setConfidence(BigDecimal.valueOf(dto.getConfidence()));
                }
                savedDates.add(keyDateRepository.save(kd));
            } catch (Exception e) {
                log.warn("Failed to persist KeyDate (date={}, type={}): {}",
                        dto.getDate(), dto.getType(), e.getMessage());
            }
        }
        log.info("Persisted {} KeyDate records for document {}", keyDateDtos.size(), document.getId());
        return savedDates;
    }

    private List<Obligation> persistObligations(Document document, List<AiObligationDto> obligationDtos) {
        List<Obligation> savedObligations = new java.util.ArrayList<>();
        for (AiObligationDto dto : obligationDtos) {
            try {
                Obligation ob = new Obligation();
                ob.setDocument(document);
                ob.setUserId(document.getUserId());
                ob.setObligationType(dto.getType());
                ob.setDescription(dto.getDescription());
                ob.setEvidence(dto.getEvidence());
                ob.setStatus(ObligationStatus.IDENTIFIED);
                if (dto.getConfidence() != null) {
                    ob.setConfidence(BigDecimal.valueOf(dto.getConfidence()));
                }
                savedObligations.add(obligationRepository.save(ob));
            } catch (Exception e) {
                log.warn("Failed to persist Obligation (type={}): {}", dto.getType(), e.getMessage());
            }
        }
        log.info("Persisted {} Obligation records for document {}", obligationDtos.size(), document.getId());
        return savedObligations;
    }

    private void failRun(AIAnalysisRun run, String rawResponse, String errorMessage) {
        try {
            run.setStatus(AnalysisRunStatus.FAILED);
            run.setRawResponse(rawResponse);
            run.setErrorMessage(errorMessage);
            run.setCompletedAt(Instant.now());
            analysisRunRepository.save(run);
        } catch (Exception e) {
            log.error("Could not update AIAnalysisRun to FAILED: {}", e.getMessage());
        }
    }

    private void markDocumentCompleted(Document document) {
        try {
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Document {} marked COMPLETED despite errors", document.getId());
        } catch (Exception e) {
            log.error("Could not mark document {} as COMPLETED: {}", document.getId(), e.getMessage());
        }
    }
}
