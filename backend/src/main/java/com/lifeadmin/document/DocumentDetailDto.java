package com.lifeadmin.document;

import com.lifeadmin.action.ActionDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed document response DTO including related key dates, obligations, and actions.
 * Used by GET /api/documents/{id} per api-spec.md.
 */
public record DocumentDetailDto(
        UUID id,
        String originalFilename,
        DocumentType documentType,
        ProcessingStatus processingStatus,
        long fileSize,
        String contentType,
        String summary,
        List<KeyDateDto> keyDates,
        List<ObligationDto> obligations,
        List<ActionDto> actions,
        Instant createdAt,
        Instant updatedAt
) {

    public record KeyDateDto(
            UUID id,
            LocalDate dateValue,
            String dateType,
            String description,
            String evidence,
            BigDecimal confidence
    ) {
        public static KeyDateDto from(KeyDate kd) {
            return new KeyDateDto(
                    kd.getId(),
                    kd.getDateValue(),
                    kd.getDateType().name(),
                    kd.getDescription(),
                    kd.getEvidence(),
                    kd.getConfidence()
            );
        }
    }

    public record ObligationDto(
            UUID id,
            String obligationType,
            String description,
            String evidence,
            BigDecimal confidence,
            String status
    ) {
        public static ObligationDto from(com.lifeadmin.obligation.Obligation obl) {
            return new ObligationDto(
                    obl.getId(),
                    obl.getObligationType(),
                    obl.getDescription(),
                    obl.getEvidence(),
                    obl.getConfidence(),
                    obl.getStatus().name()
            );
        }
    }
}
