package com.lifeadmin.document;

import com.lifeadmin.ai.AiAnalysisService;
import com.lifeadmin.ai.exception.AiProviderException;
import com.lifeadmin.ai.exception.AiResponseException;
import com.lifeadmin.extraction.DocumentText;
import com.lifeadmin.extraction.DocumentTextRepository;
import com.lifeadmin.extraction.ExtractionException;
import com.lifeadmin.extraction.PdfExtractionService;
import com.lifeadmin.storage.StorageException;
import com.lifeadmin.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the document upload pipeline:
 *
 *   validate → save Document (PROCESSING) → store PDF → extract text
 *   → persist DocumentText → mark COMPLETED
 *   [transaction commits here]
 *   → AI analysis (its own transaction)
 *
 * The AI analysis step runs AFTER the upload transaction commits so that
 * the DocumentText is visible to the AiAnalysisService's REQUIRES_NEW transaction.
 * A failure in AI analysis marks the document NEEDS_REVIEW but does NOT
 * corrupt the extracted text or roll back the upload.
 */
@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final StorageService storageService;
    private final PdfExtractionService pdfExtractionService;
    private final FileValidator fileValidator;
    private final AiAnalysisService aiAnalysisService;
    private final com.lifeadmin.action.ActionGenerationService actionGenerationService;
    private final com.lifeadmin.notification.NotificationService notificationService;

    public DocumentUploadService(
            DocumentRepository documentRepository,
            DocumentTextRepository documentTextRepository,
            StorageService storageService,
            PdfExtractionService pdfExtractionService,
            FileValidator fileValidator,
            AiAnalysisService aiAnalysisService,
            com.lifeadmin.action.ActionGenerationService actionGenerationService,
            com.lifeadmin.notification.NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.documentTextRepository = documentTextRepository;
        this.storageService = storageService;
        this.pdfExtractionService = pdfExtractionService;
        this.fileValidator = fileValidator;
        this.aiAnalysisService = aiAnalysisService;
        this.actionGenerationService = actionGenerationService;
        this.notificationService = notificationService;
    }

    /**
     * Entry point for document upload.
     * The extraction pipeline runs in a transaction; AI analysis runs AFTER
     * the transaction commits so DocumentText is visible to AiAnalysisService.
     */
    public Document upload(MultipartFile file, UUID userId) {
        // Step 1–7: validate + store + extract + persist — in one transaction
        Document document = extractAndPersist(file, userId);

        // Step 8: AI analysis — runs AFTER extraction transaction commits
        document = runAiAnalysis(document);

        return document;
    }

    /**
     * Steps 1–7: validation, storage, extraction, and persistence.
     * Runs inside a single @Transactional.
     */
    @Transactional
    public Document extractAndPersist(MultipartFile file, UUID userId) {
        // Step 1 — Validate
        fileValidator.validate(file);

        // Step 2 — Create Document record, status=PROCESSING
        Document document = new Document();
        document.setUserId(userId);
        document.setOriginalFilename(sanitiseOriginalFilename(file.getOriginalFilename()));
        document.setStoredFilename("pending");
        document.setDocumentType(DocumentType.UNKNOWN);
        document.setProcessingStatus(ProcessingStatus.PROCESSING);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document = documentRepository.save(document);

        UUID documentId = document.getId();
        log.info("Document {} created, status=PROCESSING", documentId);

        // Step 3 — Store PDF
        String storedFilename;
        try {
            storedFilename = storageService.store(file);
        } catch (StorageException e) {
            log.error("Storage failed for document {}: {}", documentId, e.getMessage());
            markFailed(document);
            throw e;
        }

        document.setStoredFilename(storedFilename);
        document = documentRepository.save(document);

        // Step 4 — Extract text
        String extractedText;
        try {
            byte[] pdfBytes = Files.readAllBytes(storageService.resolve(storedFilename));
            extractedText = pdfExtractionService.extract(pdfBytes);
        } catch (IOException | ExtractionException e) {
            log.error("Extraction failed for document {}: {}", documentId, e.getMessage());
            markFailed(document);
            storageService.delete(storedFilename);
            throw new StorageException("PDF text extraction failed", e);
        }

        // Step 5 — Handle image-only / non-text PDF
        if (!pdfExtractionService.hasExtractableText(extractedText)) {
            log.warn("Document {} has insufficient extractable text (image-only PDF?)", documentId);
            markFailed(document);
            storageService.delete(storedFilename);
            throw new InvalidDocumentException(
                    "PDF contains no extractable text. Scanned/image-only PDFs are not supported yet.");
        }

        // Step 6 — Persist DocumentText
        DocumentText documentText = new DocumentText();
        documentText.setDocument(document);
        documentText.setExtractedText(extractedText);
        documentTextRepository.save(documentText);

        // Step 7 — Mark COMPLETED
        document.setProcessingStatus(ProcessingStatus.COMPLETED);
        document = documentRepository.save(document);
        log.info("Document {} processing COMPLETED ({} chars extracted)", documentId, extractedText.length());

        return document;
    }

    /**
     * Runs AI analysis after the extraction transaction has committed.
     * Failure is non-fatal — document becomes NEEDS_REVIEW.
     */
    private Document runAiAnalysis(Document document) {
        UUID documentId = document.getId();
        try {
            java.util.Optional<com.lifeadmin.ai.dto.AiAnalysisResponse> aiResponseOpt = aiAnalysisService.analyze(document);
            
            aiResponseOpt.ifPresent(aiResponse -> {
                try {
                    actionGenerationService.generateActions(document, aiResponse);
                    notificationService.createInAppNotification(document.getUserId(), null, com.lifeadmin.notification.NotificationCategory.OBLIGATION_DETECTED, "Document processed successfully. AI analysis complete.");
                } catch (Exception e) {
                    log.warn("Action generation failed for document {}, but AI analysis succeeded: {}", documentId, e.getMessage());
                }
            });
            
            // Re-fetch to get updated documentType / summary set by AI
            return documentRepository.findById(documentId).orElse(document);
        } catch (AiProviderException | AiResponseException e) {
            log.warn("AI analysis failed for document {} (upload still succeeded): {}", documentId, e.getMessage());
            notificationService.createInAppNotification(document.getUserId(), null, com.lifeadmin.notification.NotificationCategory.OBLIGATION_DETECTED, "Document uploaded but AI analysis encountered an error.");
            return documentRepository.findById(documentId).orElse(document);
        }
    }

    /**
     * Reprocesses AI analysis for an existing document.
     * The document must already have extracted text.
     */
    @Transactional
    public Document reprocess(UUID id, UUID userId) {
        Document document = findById(id, userId);

        boolean hasText = documentTextRepository.findByDocumentId(id).isPresent();
        if (!hasText) {
            throw new InvalidDocumentException(
                    "Document " + id + " has no extracted text. Upload a valid PDF first.");
        }
        // Commit the read transaction, then run AI
        return reprocessAi(document);
    }

    /**
     * Runs AI reprocessing outside the reprocess() transaction so committed
     * DocumentText is visible.
     */
    private Document reprocessAi(Document document) {
        UUID id = document.getId();
        log.info("Reprocessing AI analysis for document {}", id);
        try {
            java.util.Optional<com.lifeadmin.ai.dto.AiAnalysisResponse> aiResponseOpt = aiAnalysisService.analyze(document);
            aiResponseOpt.ifPresent(aiResponse -> {
                try {
                    actionGenerationService.generateActions(document, aiResponse);
                } catch (Exception e) {
                    log.warn("Action generation failed for document {}, but AI analysis succeeded: {}", id, e.getMessage());
                }
            });
        } catch (AiProviderException | AiResponseException e) {
            log.warn("Reprocess AI analysis failed for document {}: {}", id, e.getMessage());
        }
        return documentRepository.findById(id).orElse(document);
    }

    // ─── query methods ────────────────────────────────────────────────────────

    public List<Document> listAll(UUID userId) {
        return documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public Document findById(UUID id, UUID userId) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + id));
        if (!doc.getUserId().equals(userId)) {
            throw new DocumentNotFoundException("Document not found: " + id); // Avoid leaking existence
        }
        return doc;
    }

    @Transactional
    public void deleteDocument(UUID id, UUID userId) {
        Document document = findById(id, userId);
        
        // Delete stored file
        try {
            if (document.getStoredFilename() != null && !document.getStoredFilename().equals("pending")) {
                storageService.delete(document.getStoredFilename());
            }
        } catch (Exception e) {
            log.warn("Failed to delete stored file for document {}: {}", id, e.getMessage());
        }

        // DB constraints ON DELETE CASCADE will handle the related entities
        documentRepository.delete(document);
        notificationService.createInAppNotification(userId, null, com.lifeadmin.notification.NotificationCategory.SYSTEM_ALERT, "Document deleted successfully.");
        log.info("Deleted document {}", id);
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private void markFailed(Document document) {
        try {
            document.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(document);
            log.info("Document {} marked FAILED", document.getId());
        } catch (Exception e) {
            log.error("Could not update document {} to FAILED status: {}", document.getId(), e.getMessage());
        }
    }

    private String sanitiseOriginalFilename(String filename) {
        if (filename == null) return "unknown.pdf";
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        return name.isBlank() ? "unknown.pdf" : name;
    }
}
