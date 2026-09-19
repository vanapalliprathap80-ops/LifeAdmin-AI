package com.lifeadmin.ai;

import com.lifeadmin.ai.dto.AiAnalysisResponse;
import com.lifeadmin.ai.dto.AiKeyDateDto;
import com.lifeadmin.ai.dto.AiObligationDto;
import com.lifeadmin.ai.exception.AiResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AiResponseValidatorTest {

    private AiResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiResponseValidator();
    }

    // ── Document type mapping ──────────────────────────────────────────────────

    @Test
    void maps_insurance_document_type() {
        assertThat(validator.validateDocumentType("INSURANCE")).isEqualTo("INSURANCE");
        assertThat(validator.validateDocumentType("insurance")).isEqualTo("INSURANCE");
        assertThat(validator.validateDocumentType("INSURANCE_POLICY")).isEqualTo("INSURANCE");
    }

    @Test
    void maps_lease_document_type() {
        assertThat(validator.validateDocumentType("LEASE")).isEqualTo("LEASE");
        assertThat(validator.validateDocumentType("RENTAL_AGREEMENT")).isEqualTo("LEASE");
        assertThat(validator.validateDocumentType("rental")).isEqualTo("LEASE");
    }

    @Test
    void maps_bill_document_type() {
        assertThat(validator.validateDocumentType("BILL")).isEqualTo("BILL");
        assertThat(validator.validateDocumentType("INVOICE")).isEqualTo("BILL");
        assertThat(validator.validateDocumentType("BILL_INVOICE")).isEqualTo("BILL");
    }

    @Test
    void maps_warranty_document_type() {
        assertThat(validator.validateDocumentType("WARRANTY")).isEqualTo("WARRANTY");
    }

    @Test
    void maps_subscription_document_type() {
        assertThat(validator.validateDocumentType("SUBSCRIPTION")).isEqualTo("SUBSCRIPTION");
    }

    @Test
    void defaults_unknown_for_unrecognized_type() {
        assertThat(validator.validateDocumentType("COMPLETELY_UNKNOWN_TYPE")).isEqualTo("UNKNOWN");
        assertThat(validator.validateDocumentType("")).isEqualTo("UNKNOWN");
        assertThat(validator.validateDocumentType(null)).isEqualTo("UNKNOWN");
    }

    // ── Key date type mapping ──────────────────────────────────────────────────

    @Test
    void maps_expiry_date_type() {
        assertThat(validator.validateKeyDateType("EXPIRY_DATE")).isEqualTo("EXPIRY_DATE");
        assertThat(validator.validateKeyDateType("EXPIRATION_DATE")).isEqualTo("EXPIRY_DATE");
        assertThat(validator.validateKeyDateType("EXPIRY")).isEqualTo("EXPIRY_DATE");
    }

    @Test
    void maps_start_date_type() {
        assertThat(validator.validateKeyDateType("START_DATE")).isEqualTo("START_DATE");
        assertThat(validator.validateKeyDateType("ISSUE_DATE")).isEqualTo("START_DATE");
    }

    @Test
    void maps_payment_due_type() {
        assertThat(validator.validateKeyDateType("PAYMENT_DUE")).isEqualTo("PAYMENT_DUE");
        assertThat(validator.validateKeyDateType("DUE_DATE")).isEqualTo("PAYMENT_DUE");
    }

    @Test
    void defaults_other_for_unknown_date_type() {
        assertThat(validator.validateKeyDateType("SOME_WEIRD_TYPE")).isEqualTo("OTHER");
        assertThat(validator.validateKeyDateType(null)).isEqualTo("OTHER");
    }

    // ── Confidence clamping ────────────────────────────────────────────────────

    @Test
    void clamps_confidence_above_1() {
        assertThat(validator.clampConfidence(1.5)).isEqualTo(1.0);
    }

    @Test
    void clamps_confidence_below_0() {
        assertThat(validator.clampConfidence(-0.5)).isEqualTo(0.0);
    }

    @Test
    void defaults_confidence_when_null() {
        assertThat(validator.clampConfidence(null)).isEqualTo(0.5);
    }

    @Test
    void keeps_valid_confidence_unchanged() {
        assertThat(validator.clampConfidence(0.95)).isEqualTo(0.95);
    }

    // ── Full validation ────────────────────────────────────────────────────────

    @Test
    void validates_full_valid_response() {
        AiAnalysisResponse response = buildValidResponse();
        AiAnalysisResponse result = validator.validate(response);

        assertThat(result.getDocumentType()).isEqualTo("INSURANCE");
        assertThat(result.getKeyDates()).hasSize(1);
        assertThat(result.getObligations()).hasSize(1);
    }

    @Test
    void removes_key_date_with_invalid_date_format() {
        AiAnalysisResponse response = buildValidResponse();
        AiKeyDateDto bad = new AiKeyDateDto();
        bad.setDate("not-a-date");
        bad.setType("EXPIRY_DATE");
        bad.setDescription("Bad date");
        bad.setConfidence(0.9);
        response.getKeyDates().add(bad);

        AiAnalysisResponse result = validator.validate(response);
        // Only the valid date should remain
        assertThat(result.getKeyDates()).hasSize(1);
    }

    @Test
    void removes_key_date_with_missing_date() {
        AiAnalysisResponse response = buildValidResponse();
        AiKeyDateDto bad = new AiKeyDateDto();
        bad.setType("EXPIRY_DATE");
        bad.setDescription("Missing date value");
        bad.setConfidence(0.9);
        // date is null
        response.getKeyDates().add(bad);

        AiAnalysisResponse result = validator.validate(response);
        assertThat(result.getKeyDates()).hasSize(1);
    }

    @Test
    void removes_obligation_with_missing_description() {
        AiAnalysisResponse response = buildValidResponse();
        AiObligationDto bad = new AiObligationDto();
        bad.setType("SOME_TYPE");
        // description is null
        response.getObligations().add(bad);

        AiAnalysisResponse result = validator.validate(response);
        assertThat(result.getObligations()).hasSize(1);
    }

    @Test
    void handles_null_lists_by_defaulting_to_empty() {
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setDocumentType("UNKNOWN");
        response.setKeyDates(null);
        response.setObligations(null);
        response.setDateRelationships(null);
        response.setUncertainties(null);

        AiAnalysisResponse result = validator.validate(response);
        assertThat(result.getKeyDates()).isEmpty();
        assertThat(result.getObligations()).isEmpty();
        assertThat(result.getDateRelationships()).isEmpty();
        assertThat(result.getUncertainties()).isEmpty();
    }

    @Test
    void throws_on_null_response() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(AiResponseException.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AiAnalysisResponse buildValidResponse() {
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setDocumentType("INSURANCE");
        response.setSummary("Home insurance policy.");

        AiKeyDateDto kd = new AiKeyDateDto();
        kd.setType("EXPIRY_DATE");
        kd.setDate("2026-10-14");
        kd.setDescription("Policy expiry");
        kd.setEvidence("Expires 14 October 2026");
        kd.setConfidence(0.97);

        AiObligationDto ob = new AiObligationDto();
        ob.setType("RENEW_POLICY");
        ob.setDescription("Renew policy before expiry");
        ob.setEvidence("Policy expires 14 October 2026");
        ob.setConfidence(0.94);

        response.setKeyDates(new ArrayList<>(List.of(kd)));
        response.setObligations(new ArrayList<>(List.of(ob)));
        response.setDateRelationships(new ArrayList<>());
        response.setUncertainties(new ArrayList<>());
        return response;
    }
}
