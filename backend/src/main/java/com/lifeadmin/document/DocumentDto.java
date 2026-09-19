package com.lifeadmin.document;

import java.time.Instant;
import java.util.UUID;

/**
 * API response DTO for document metadata.
 * Never includes filesystem paths, extracted text, or stack traces.
 */
public record DocumentDto(
        UUID id,
        String originalFilename,
        DocumentType documentType,
        ProcessingStatus processingStatus,
        long fileSize,
        String contentType,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentDto from(Document doc) {
        return new DocumentDto(
                doc.getId(),
                doc.getOriginalFilename(),
                doc.getDocumentType(),
                doc.getProcessingStatus(),
                doc.getFileSize(),
                doc.getContentType(),
                doc.getSummary(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
