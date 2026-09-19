package com.lifeadmin.deadline;

import com.lifeadmin.document.KeyDate;
import com.lifeadmin.document.KeyDateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineCalculatorTest {

    private DeadlineCalculator calculator;
    private KeyDate expiryDate;
    private KeyDate startDate;

    @BeforeEach
    void setUp() {
        calculator = new DeadlineCalculator();

        expiryDate = new KeyDate();
        expiryDate.setDateType(KeyDateType.EXPIRY_DATE);
        expiryDate.setDateValue(LocalDate.of(2026, 12, 31));

        startDate = new KeyDate();
        startDate.setDateType(KeyDateType.START_DATE);
        startDate.setDateValue(LocalDate.of(2026, 1, 1));
    }

    @Test
    void testNoticePeriodCalculation_success() {
        // AI says: NOTICE_PERIOD_BEFORE_EXPIRY, offset 60, computed 2026-11-01
        DeadlineResult result = calculator.calculate(
                "NOTICE_PERIOD_BEFORE_EXPIRY",
                "EXPIRY_DATE",
                60,
                "2026-11-01",
                List.of(expiryDate)
        );

        assertThat(result.isResolved()).isTrue();
        assertThat(result.getCalculatedDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(result.isAiMismatch()).isFalse();
    }

    @Test
    void testNoticePeriodCalculation_aiMismatch() {
        // AI incorrectly computes 2026-11-05
        DeadlineResult result = calculator.calculate(
                "NOTICE_PERIOD_BEFORE_EXPIRY",
                "EXPIRY_DATE",
                60,
                "2026-11-05", // Wrong!
                List.of(expiryDate)
        );

        assertThat(result.isResolved()).isTrue();
        assertThat(result.getCalculatedDate()).isEqualTo(LocalDate.of(2026, 11, 1)); // Authoritative backend result
        assertThat(result.isAiMismatch()).isTrue();
        assertThat(result.getAiComputedDate()).isEqualTo("2026-11-05");
        assertThat(result.getValidationNote()).contains("AI computed 2026-11-05 but backend calculated 2026-11-01");
    }

    @Test
    void testPaymentDueCalculation() {
        DeadlineResult result = calculator.calculate(
                "PAYMENT_DUE_AFTER_INVOICE_DATE",
                "START_DATE",
                14,
                "2026-01-15",
                List.of(startDate)
        );

        assertThat(result.isResolved()).isTrue();
        assertThat(result.getCalculatedDate()).isEqualTo(LocalDate.of(2026, 1, 15)); // 1 + 14 = 15
        assertThat(result.isAiMismatch()).isFalse();
    }

    @Test
    void testMissingAnchorDate() {
        DeadlineResult result = calculator.calculate(
                "NOTICE_PERIOD_BEFORE_EXPIRY",
                "EXPIRY_DATE",
                60,
                "2026-11-01",
                List.of(startDate) // No expiry date here
        );

        assertThat(result.isResolved()).isFalse();
        assertThat(result.getCalculatedDate()).isNull();
        assertThat(result.getValidationNote()).contains("Missing anchor date: EXPIRY_DATE");
    }

    @Test
    void testUnsupportedRelationship() {
        DeadlineResult result = calculator.calculate(
                "UNKNOWN_RELATIONSHIP",
                "EXPIRY_DATE",
                60,
                "2026-11-01",
                List.of(expiryDate)
        );

        assertThat(result.isResolved()).isFalse();
        assertThat(result.getValidationNote()).contains("Unsupported relationship type: UNKNOWN_RELATIONSHIP");
    }

    @Test
    void testInvalidOffset() {
        DeadlineResult result = calculator.calculate(
                "NOTICE_PERIOD_BEFORE_EXPIRY",
                "EXPIRY_DATE",
                -5, // invalid
                null,
                List.of(expiryDate)
        );

        assertThat(result.isResolved()).isFalse();
        assertThat(result.getValidationNote()).contains("Invalid offset: -5");
    }

    @Test
    void testResolveDirectDate() {
        Optional<LocalDate> date = calculator.resolveDirectDate("EXPIRY_DATE", List.of(startDate, expiryDate));
        assertThat(date).isPresent();
        assertThat(date.get()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
