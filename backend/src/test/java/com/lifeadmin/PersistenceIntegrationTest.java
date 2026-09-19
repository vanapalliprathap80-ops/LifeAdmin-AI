package com.lifeadmin;

import com.lifeadmin.action.*;
import com.lifeadmin.ai.*;
import com.lifeadmin.document.*;
import com.lifeadmin.extraction.*;
import com.lifeadmin.obligation.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Phase 2 persistence layer.
 * These tests use the real local PostgreSQL database.
 * Each test is transactional and rolled back after execution to avoid
 * polluting the development database.
 */
@SpringBootTest
@Transactional
@Rollback
class PersistenceIntegrationTest {

    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentTextRepository documentTextRepository;
    @Autowired KeyDateRepository keyDateRepository;
    @Autowired ObligationRepository obligationRepository;
    @Autowired ActionRepository actionRepository;
    @Autowired AIAnalysisRunRepository aiAnalysisRunRepository;

    // ── helper: persist a valid Document ──────────────────────────────────────
    private Document buildAndSaveDocument() {
        Document doc = new Document();
        doc.setOriginalFilename("lease_agreement.pdf");
        doc.setStoredFilename("stored_lease_agreement.pdf");
        doc.setDocumentType(DocumentType.LEASE);
        doc.setProcessingStatus(ProcessingStatus.UPLOADED);
        doc.setFileSize(204800L);
        doc.setContentType("application/pdf");
        return documentRepository.save(doc);
    }

    // ── Document ──────────────────────────────────────────────────────────────

    @Test
    void document_canBePersisted() {
        Document saved = buildAndSaveDocument();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Document found = documentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOriginalFilename()).isEqualTo("lease_agreement.pdf");
        assertThat(found.getDocumentType()).isEqualTo(DocumentType.LEASE);
        assertThat(found.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
    }

    // ── Document → DocumentText (1-1) ─────────────────────────────────────────

    @Test
    void documentText_canBePersisted_andLinkedToDocument() {
        Document doc = buildAndSaveDocument();

        DocumentText text = new DocumentText();
        text.setDocument(doc);
        text.setExtractedText("This lease agreement is entered into on 1st January 2026...");
        DocumentText saved = documentTextRepository.save(text);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        DocumentText found = documentTextRepository.findByDocumentId(doc.getId()).orElseThrow();
        assertThat(found.getExtractedText()).contains("lease agreement");
    }

    // ── Document → KeyDate (1-N) ──────────────────────────────────────────────

    @Test
    void keyDate_canBePersisted_andRetrievedByDocument() {
        Document doc = buildAndSaveDocument();

        KeyDate kd = new KeyDate();
        kd.setDocument(doc);
        kd.setDateValue(LocalDate.of(2026, 12, 31));
        kd.setDateType(KeyDateType.RENEWAL_DATE);
        kd.setDescription("Annual lease renewal date");
        kd.setEvidence("...this agreement shall renew on December 31, 2026...");
        kd.setConfidence(BigDecimal.valueOf(0.92));
        keyDateRepository.save(kd);

        var found = keyDateRepository.findByDocumentId(doc.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getDateType()).isEqualTo(KeyDateType.RENEWAL_DATE);
        assertThat(found.get(0).getDateValue()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    // ── Document → Obligation (1-N) ───────────────────────────────────────────

    @Test
    void obligation_canBePersisted_andRetrievedByDocument() {
        Document doc = buildAndSaveDocument();

        Obligation obl = new Obligation();
        obl.setDocument(doc);
        obl.setObligationType("PAYMENT");
        obl.setDescription("Pay monthly rent of $1,500 by the 1st of each month");
        obl.setEvidence("Tenant shall pay rent of $1,500 on or before the 1st day of each month.");
        obl.setConfidence(BigDecimal.valueOf(0.98));
        obl.setStatus(ObligationStatus.IDENTIFIED);
        obligationRepository.save(obl);

        var found = obligationRepository.findByDocumentId(doc.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getObligationType()).isEqualTo("PAYMENT");
        assertThat(found.get(0).getStatus()).isEqualTo(ObligationStatus.IDENTIFIED);
    }

    // ── Document → Action (1-N) and Obligation → Action (1-N) ────────────────

    @Test
    void action_canBePersisted_withDocumentAndObligation() {
        Document doc = buildAndSaveDocument();

        Obligation obl = new Obligation();
        obl.setDocument(doc);
        obl.setDescription("Pay monthly rent");
        obl.setStatus(ObligationStatus.IDENTIFIED);
        obligationRepository.save(obl);

        Action action = new Action();
        action.setDocument(doc);
        action.setObligation(obl);
        action.setTitle("Pay October rent");
        action.setDescription("Submit $1,500 rent payment for October 2026");
        action.setRecommendedDate(LocalDate.of(2026, 10, 1));
        action.setDeadline(LocalDate.of(2026, 10, 5));
        action.setPriority(ActionPriority.HIGH);
        action.setStatus(ActionStatus.PENDING);
        Action saved = actionRepository.save(action);

        assertThat(saved.getId()).isNotNull();

        var byDoc = actionRepository.findByDocumentId(doc.getId());
        assertThat(byDoc).hasSize(1);
        assertThat(byDoc.get(0).getTitle()).isEqualTo("Pay October rent");
        assertThat(byDoc.get(0).getPriority()).isEqualTo(ActionPriority.HIGH);

        var byObl = actionRepository.findByObligationId(obl.getId());
        assertThat(byObl).hasSize(1);
    }

    @Test
    void action_canBePersisted_withoutObligation() {
        Document doc = buildAndSaveDocument();

        Action action = new Action();
        action.setDocument(doc);
        action.setTitle("Review insurance terms");
        action.setStatus(ActionStatus.PENDING);
        action.setPriority(ActionPriority.MEDIUM);
        actionRepository.save(action);

        var found = actionRepository.findByDocumentId(doc.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getObligation()).isNull();
    }

    // ── Document → AIAnalysisRun (1-N) ───────────────────────────────────────

    @Test
    void aiAnalysisRun_canBePersisted_andRetrievedByDocument() {
        Document doc = buildAndSaveDocument();

        AIAnalysisRun run = new AIAnalysisRun();
        run.setDocument(doc);
        run.setProvider("Google");
        run.setModel("gemini-2.0-flash");
        run.setStatus(AnalysisRunStatus.COMPLETED);
        run.setRawResponse("{\"obligations\": []}");
        aiAnalysisRunRepository.save(run);

        var runs = aiAnalysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(doc.getId());
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getStatus()).isEqualTo(AnalysisRunStatus.COMPLETED);
        assertThat(runs.get(0).getProvider()).isEqualTo("Google");
    }

    @Test
    void aiAnalysisRun_failedRunStoresErrorMessage() {
        Document doc = buildAndSaveDocument();

        AIAnalysisRun run = new AIAnalysisRun();
        run.setDocument(doc);
        run.setProvider("Google");
        run.setStatus(AnalysisRunStatus.FAILED);
        run.setErrorMessage("Quota exceeded");
        aiAnalysisRunRepository.save(run);

        var runs = aiAnalysisRunRepository.findByDocumentIdOrderByCreatedAtDesc(doc.getId());
        assertThat(runs.get(0).getStatus()).isEqualTo(AnalysisRunStatus.FAILED);
        assertThat(runs.get(0).getErrorMessage()).isEqualTo("Quota exceeded");
    }
}
