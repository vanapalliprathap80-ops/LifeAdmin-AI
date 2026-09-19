package com.lifeadmin.document;

import com.lifeadmin.action.ActionDto;
import com.lifeadmin.action.ActionRepository;
import com.lifeadmin.auth.User;
import com.lifeadmin.common.ApiResponse;
import com.lifeadmin.obligation.ObligationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Handles HTTP concerns only. No filesystem or PDF logic lives here.
 * Business logic is delegated to DocumentUploadService.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentUploadService documentUploadService;
    private final KeyDateRepository keyDateRepository;
    private final ObligationRepository obligationRepository;
    private final ActionRepository actionRepository;

    public DocumentController(DocumentUploadService documentUploadService,
                              KeyDateRepository keyDateRepository,
                              ObligationRepository obligationRepository,
                              ActionRepository actionRepository) {
        this.documentUploadService = documentUploadService;
        this.keyDateRepository = keyDateRepository;
        this.obligationRepository = obligationRepository;
        this.actionRepository = actionRepository;
    }

    /**
     * POST /api/documents
     * Upload a PDF document (validates, extracts, and runs AI analysis).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        log.info("Received upload request: filename={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        Document saved = documentUploadService.upload(file, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(DocumentDto.from(saved)));
    }

    /**
     * GET /api/documents
     * List all documents (metadata only, no extracted text).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentDto>>> listAll(@AuthenticationPrincipal User user) {
        List<DocumentDto> docs = documentUploadService.listAll(user.getId())
                .stream()
                .map(DocumentDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    /**
     * GET /api/documents/{id}
     * Get a single document's detail including key dates, obligations, and actions.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDetailDto>> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Document doc = documentUploadService.findById(id, user.getId());

        var keyDates = keyDateRepository.findByDocumentId(id).stream()
                .map(DocumentDetailDto.KeyDateDto::from).toList();
        var obligations = obligationRepository.findByUserIdAndDocumentId(user.getId(), id).stream()
                .map(DocumentDetailDto.ObligationDto::from).toList();
        var actions = actionRepository.findByUserIdAndDocumentId(user.getId(), id).stream()
                .map(ActionDto::fromEntity).toList();

        DocumentDetailDto detail = new DocumentDetailDto(
                doc.getId(), doc.getOriginalFilename(), doc.getDocumentType(),
                doc.getProcessingStatus(), doc.getFileSize(), doc.getContentType(),
                doc.getSummary(), keyDates, obligations, actions,
                doc.getCreatedAt(), doc.getUpdatedAt()
        );
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /**
     * GET /api/documents/{id}/status
     * Get a document's processing status.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DocumentDto>> getStatus(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Document doc = documentUploadService.findById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(DocumentDto.from(doc)));
    }

    /**
     * POST /api/documents/{id}/reprocess
     * Re-run AI analysis on an existing document (per api-spec.md).
     * Returns 202 Accepted with updated document status.
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<ApiResponse<DocumentDto>> reprocess(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        log.info("Received reprocess request for document {}", id);
        Document updated = documentUploadService.reprocess(id, user.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(DocumentDto.from(updated)));
    }

    /**
     * DELETE /api/documents/{id}
     * Delete a document and its associated data.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        log.info("Received delete request for document {}", id);
        documentUploadService.deleteDocument(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}

