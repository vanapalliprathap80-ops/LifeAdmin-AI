package com.lifeadmin.action;

import com.lifeadmin.deadline.DeadlineResult;
import com.lifeadmin.deadline.PriorityCalculator;
import com.lifeadmin.document.Document;
import com.lifeadmin.obligation.Obligation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionFactoryTest {

    private ActionFactory factory;
    private PriorityCalculator mockPriorityCalculator;
    private Document doc;
    private Obligation obligation;

    @BeforeEach
    void setUp() {
        mockPriorityCalculator = mock(PriorityCalculator.class);
        when(mockPriorityCalculator.calculate(any())).thenReturn(ActionPriority.HIGH);
        
        factory = new ActionFactory(mockPriorityCalculator);
        
        doc = new Document();
        
        obligation = new Obligation();
        obligation.setDocument(doc);
        obligation.setObligationType("RENEW_POLICY");
        obligation.setDescription("Must renew the insurance policy");
        obligation.setEvidence("Section 4 states policy must be renewed");
    }

    @Test
    void testCreateAction_resolvedDeadline() {
        DeadlineResult deadline = DeadlineResult.success(LocalDate.of(2026, 10, 1));
        
        Action action = factory.createAction(doc, obligation, deadline);
        
        assertThat(action.getDocument()).isEqualTo(doc);
        assertThat(action.getObligation()).isEqualTo(obligation);
        assertThat(action.getTitle()).isEqualTo("Renew policy");
        assertThat(action.getDescription()).isEqualTo("Must renew the insurance policy");
        assertThat(action.getDeadline()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(action.getRecommendedDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(action.getPriority()).isEqualTo(ActionPriority.HIGH);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING);
        assertThat(action.getEvidence()).isEqualTo("Section 4 states policy must be renewed");
    }

    @Test
    void testCreateAction_aiMismatch() {
        DeadlineResult deadline = DeadlineResult.mismatch(LocalDate.of(2026, 10, 1), "2026-10-05");
        
        Action action = factory.createAction(doc, obligation, deadline);
        
        assertThat(action.getDeadline()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(action.getStatus()).isEqualTo(ActionStatus.NEEDS_REVIEW);
        assertThat(action.getReason()).contains("AI mismatch");
    }

    @Test
    void testCreateAction_unresolvedDeadline() {
        DeadlineResult deadline = DeadlineResult.unresolved("Missing anchor date");
        
        Action action = factory.createAction(doc, obligation, deadline);
        
        assertThat(action.getDeadline()).isNull();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.NEEDS_REVIEW);
        assertThat(action.getReason()).contains("Could not calculate deadline safely");
    }

    @Test
    void testCreateAction_nullDeadline() {
        // e.g. obligation didn't even have a relatedDateType
        Action action = factory.createAction(doc, obligation, null);
        
        assertThat(action.getDeadline()).isNull();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING);
        assertThat(action.getReason()).isEqualTo("Extracted from document obligation.");
    }
}
