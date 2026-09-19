package com.lifeadmin.deadline;

import com.lifeadmin.document.KeyDate;
import com.lifeadmin.document.KeyDateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic date arithmetic engine.
 *
 * The LLM interprets documents and extracts date relationships.
 * This class performs the actual calculation and validates against AI claims.
 * The backend result is always authoritative.
 */
@Component
public class DeadlineCalculator {

    private static final Logger log = LoggerFactory.getLogger(DeadlineCalculator.class);

    // Maps relationship type → (direction, expected anchor type)
    // direction: true = subtract offsetDays, false = add offsetDays
    private static final Map<String, RelationshipRule> SUPPORTED_RELATIONSHIPS = Map.of(
            "NOTICE_PERIOD_BEFORE_EXPIRY",
            new RelationshipRule(KeyDateType.EXPIRY_DATE, true),

            "PAYMENT_DUE_AFTER_INVOICE_DATE",
            new RelationshipRule(KeyDateType.START_DATE, false),

            "WARRANTY_END_AFTER_PURCHASE_DATE",
            new RelationshipRule(KeyDateType.START_DATE, false)
    );

    /**
     * Calculates a deadline from a date relationship.
     *
     * @param relationshipType  e.g. "NOTICE_PERIOD_BEFORE_EXPIRY"
     * @param anchorDateType    e.g. "EXPIRY_DATE"
     * @param offsetDays        e.g. 60
     * @param aiComputedDate    the AI's claimed result (may be null)
     * @param keyDates          all key dates for the document
     * @return a DeadlineResult: resolved (possibly with mismatch) or unresolved
     */
    public DeadlineResult calculate(String relationshipType, String anchorDateType,
                                    Integer offsetDays, String aiComputedDate,
                                    List<KeyDate> keyDates) {

        // Step 1 — Validate relationship type is supported
        RelationshipRule rule = SUPPORTED_RELATIONSHIPS.get(relationshipType);
        if (rule == null) {
            log.warn("Unsupported relationship type: {}", relationshipType);
            return DeadlineResult.unresolved(
                    "Unsupported relationship type: " + relationshipType);
        }

        // Step 2 — Validate offsetDays
        if (offsetDays == null || offsetDays < 0) {
            log.warn("Invalid offsetDays={} for relationship {}", offsetDays, relationshipType);
            return DeadlineResult.unresolved(
                    "Invalid offset: " + offsetDays + " for " + relationshipType);
        }

        // Step 3 — Resolve anchor date type
        KeyDateType resolvedAnchorType;
        try {
            resolvedAnchorType = KeyDateType.valueOf(anchorDateType);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Unknown anchor date type: {}", anchorDateType);
            return DeadlineResult.unresolved(
                    "Unknown anchor date type: " + anchorDateType);
        }

        // Step 4 — Find the anchor date in the document's key dates
        Optional<LocalDate> anchorDateOpt = keyDates.stream()
                .filter(kd -> kd.getDateType() == resolvedAnchorType)
                .map(KeyDate::getDateValue)
                .findFirst();

        if (anchorDateOpt.isEmpty()) {
            log.warn("Missing anchor date {} for relationship {}",
                    resolvedAnchorType, relationshipType);
            return DeadlineResult.unresolved(
                    "Missing anchor date: " + resolvedAnchorType);
        }

        LocalDate anchorDate = anchorDateOpt.get();

        // Step 5 — Deterministic calculation
        LocalDate calculated = rule.subtract()
                ? anchorDate.minusDays(offsetDays)
                : anchorDate.plusDays(offsetDays);

        log.info("Calculated deadline: {} {} {} days = {} (relationship={})",
                anchorDate, rule.subtract() ? "-" : "+", offsetDays, calculated, relationshipType);

        // Step 6 — Compare with AI's computedDate
        if (aiComputedDate != null && !aiComputedDate.isBlank()) {
            try {
                LocalDate aiDate = LocalDate.parse(aiComputedDate);
                if (!aiDate.equals(calculated)) {
                    log.warn("AI MISMATCH: AI computed {} but backend calculated {} for {}",
                            aiComputedDate, calculated, relationshipType);
                    return DeadlineResult.mismatch(calculated, aiComputedDate);
                }
            } catch (DateTimeParseException e) {
                log.warn("AI computedDate '{}' is not a valid date, ignoring", aiComputedDate);
                // AI date is invalid, but our calculation is fine
            }
        }

        return DeadlineResult.success(calculated);
    }

    /**
     * Resolves a direct deadline from a KeyDate (no relationship needed).
     * Used for obligations that directly reference a date type
     * (e.g. RENEW_POLICY → EXPIRY_DATE).
     */
    public Optional<LocalDate> resolveDirectDate(String dateType, List<KeyDate> keyDates) {
        if (dateType == null || dateType.isBlank()) {
            return Optional.empty();
        }
        try {
            KeyDateType type = KeyDateType.valueOf(dateType);
            return keyDates.stream()
                    .filter(kd -> kd.getDateType() == type)
                    .map(KeyDate::getDateValue)
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isSupported(String relationshipType) {
        return SUPPORTED_RELATIONSHIPS.containsKey(relationshipType);
    }

    /**
     * Rule definition for a supported relationship.
     */
    private record RelationshipRule(KeyDateType expectedAnchorType, boolean subtract) {}
}
