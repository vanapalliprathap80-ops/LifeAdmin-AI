package com.lifeadmin.ai;

import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiKeyDateDto;
import com.lifeadmin.ai.dto.AiObligationDto;
import com.lifeadmin.ai.exception.AiResponseException;
import com.lifeadmin.document.DocumentType;
import com.lifeadmin.document.KeyDateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Validates a parsed AiAnalysisResponse.
 * Invalid individual items are skipped with warnings rather than
 * failing the entire analysis, unless the response is fundamentally broken.
 */
@Component
public class AiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(AiResponseValidator.class);

    /**
     * Validates and sanitizes the AI response in place.
     * Returns the validated response with invalid entries removed.
     *
     * @throws AiResponseException if the response is fundamentally invalid
     */
    public AiAnalysisResponse validate(AiAnalysisResponse response) {
        if (response == null) {
            throw new AiResponseException("AI response is null");
        }

        // Validate and map document type
        response.setDocumentType(validateDocumentType(response.getDocumentType()));

        // Ensure lists are not null
        if (response.getKeyDates() == null) {
            response.setKeyDates(Collections.emptyList());
        }
        if (response.getObligations() == null) {
            response.setObligations(Collections.emptyList());
        }
        if (response.getDateRelationships() == null) {
            response.setDateRelationships(Collections.emptyList());
        }
        if (response.getUncertainties() == null) {
            response.setUncertainties(Collections.emptyList());
        }

        // Validate key dates — remove invalid entries
        List<AiKeyDateDto> validDates = new ArrayList<>();
        for (AiKeyDateDto kd : response.getKeyDates()) {
            if (validateKeyDate(kd)) {
                kd.setConfidence(clampConfidence(kd.getConfidence()));
                validDates.add(kd);
            }
        }
        response.setKeyDates(validDates);

        // Validate obligations — remove entries without description
        List<AiObligationDto> validObligations = new ArrayList<>();
        for (AiObligationDto ob : response.getObligations()) {
            if (validateObligation(ob)) {
                ob.setConfidence(clampConfidence(ob.getConfidence()));
                validObligations.add(ob);
            }
        }
        response.setObligations(validObligations);

        // Validate date relationships — clamp confidence
        response.getDateRelationships().forEach(dr -> {
            dr.setConfidence(clampConfidence(dr.getConfidence()));
        });

        log.info("Validated AI response: documentType={}, keyDates={}, obligations={}, relationships={}",
                response.getDocumentType(),
                response.getKeyDates().size(),
                response.getObligations().size(),
                response.getDateRelationships().size());

        return response;
    }

    /**
     * Maps the AI's documentType string to a valid DocumentType enum value.
     * Returns "UNKNOWN" for unrecognized values.
     */
    String validateDocumentType(String type) {
        if (type == null || type.isBlank()) {
            return DocumentType.UNKNOWN.name();
        }

        // Normalize: uppercase, replace spaces/hyphens
        String normalized = type.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");

        // Handle common AI variations
        if (normalized.contains("INSURANCE")) return DocumentType.INSURANCE.name();
        if (normalized.contains("LEASE") || normalized.contains("RENTAL")) return DocumentType.LEASE.name();
        if (normalized.contains("BILL") || normalized.contains("INVOICE")) return DocumentType.BILL.name();
        if (normalized.contains("WARRANTY")) return DocumentType.WARRANTY.name();
        if (normalized.contains("SUBSCRIPTION")) return DocumentType.SUBSCRIPTION.name();

        try {
            return DocumentType.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown document type from AI: '{}', defaulting to UNKNOWN", type);
            return DocumentType.UNKNOWN.name();
        }
    }

    /**
     * Validates a key date entry. Returns false if the entry should be skipped.
     */
    private boolean validateKeyDate(AiKeyDateDto kd) {
        if (kd.getDate() == null || kd.getDate().isBlank()) {
            log.warn("Skipping key date with missing date value");
            return false;
        }

        try {
            LocalDate.parse(kd.getDate());
        } catch (DateTimeParseException e) {
            log.warn("Skipping key date with invalid date format: '{}'", kd.getDate());
            return false;
        }

        // Validate date type — default to OTHER if unrecognized
        if (kd.getType() != null) {
            kd.setType(validateKeyDateType(kd.getType()));
        } else {
            kd.setType(KeyDateType.OTHER.name());
        }

        return true;
    }

    /**
     * Maps the AI's date type string to a valid KeyDateType enum value.
     */
    String validateKeyDateType(String type) {
        if (type == null || type.isBlank()) return KeyDateType.OTHER.name();

        String normalized = type.trim().toUpperCase().replace(" ", "_").replace("-", "_");

        // Handle common AI variations
        if (normalized.contains("EXPIR")) return KeyDateType.EXPIRY_DATE.name();
        if (normalized.contains("START") || normalized.equals("ISSUE_DATE")) return KeyDateType.START_DATE.name();
        if (normalized.contains("END") && !normalized.contains("NOTICE")) return KeyDateType.END_DATE.name();
        if (normalized.contains("RENEWAL")) return KeyDateType.RENEWAL_DATE.name();
        if (normalized.contains("PAYMENT") || normalized.contains("DUE")) return KeyDateType.PAYMENT_DUE.name();
        if (normalized.contains("EFFECTIVE")) return KeyDateType.EFFECTIVE_DATE.name();
        if (normalized.contains("REVIEW")) return KeyDateType.REVIEW_DATE.name();
        if (normalized.contains("CANCELLATION")) return KeyDateType.CANCELLATION_DEADLINE.name();
        if (normalized.contains("NOTICE")) return KeyDateType.NOTICE_PERIOD_END.name();

        try {
            return KeyDateType.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown key date type from AI: '{}', defaulting to OTHER", type);
            return KeyDateType.OTHER.name();
        }
    }

    /**
     * Validates an obligation entry. Returns false if the entry should be skipped.
     */
    private boolean validateObligation(AiObligationDto ob) {
        if (ob.getDescription() == null || ob.getDescription().isBlank()) {
            log.warn("Skipping obligation with missing description");
            return false;
        }
        return true;
    }

    /**
     * Clamps confidence to 0.0-1.0 range. Defaults to 0.5 if null.
     */
    double clampConfidence(Double confidence) {
        if (confidence == null) return 0.5;
        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
