package com.lifeadmin.deadline;

import java.time.LocalDate;

/**
 * Immutable value object holding the result of a deterministic deadline calculation.
 * Captures both the authoritative backend result and any AI mismatch diagnostics.
 */
public final class DeadlineResult {

    private final LocalDate calculatedDate;
    private final boolean aiMismatch;
    private final String aiComputedDate;
    private final String validationNote;

    private DeadlineResult(LocalDate calculatedDate, boolean aiMismatch,
                           String aiComputedDate, String validationNote) {
        this.calculatedDate = calculatedDate;
        this.aiMismatch = aiMismatch;
        this.aiComputedDate = aiComputedDate;
        this.validationNote = validationNote;
    }

    /** Successful calculation with no AI mismatch. */
    public static DeadlineResult success(LocalDate calculatedDate) {
        return new DeadlineResult(calculatedDate, false, null, null);
    }

    /** Successful calculation where AI's computed date disagrees. */
    public static DeadlineResult mismatch(LocalDate calculatedDate, String aiComputedDate) {
        return new DeadlineResult(calculatedDate, true, aiComputedDate,
                "AI computed " + aiComputedDate + " but backend calculated " + calculatedDate);
    }

    /** Calculation failed — no reliable date could be derived. */
    public static DeadlineResult unresolved(String reason) {
        return new DeadlineResult(null, false, null, reason);
    }

    public LocalDate getCalculatedDate() { return calculatedDate; }
    public boolean isAiMismatch() { return aiMismatch; }
    public String getAiComputedDate() { return aiComputedDate; }
    public String getValidationNote() { return validationNote; }
    public boolean isResolved() { return calculatedDate != null; }

    @Override
    public String toString() {
        if (calculatedDate == null) {
            return "DeadlineResult[UNRESOLVED: " + validationNote + "]";
        }
        return "DeadlineResult[" + calculatedDate +
                (aiMismatch ? ", AI_MISMATCH(" + aiComputedDate + ")" : "") + "]";
    }
}
