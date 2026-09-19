package com.lifeadmin.action;

import com.lifeadmin.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActionServiceTest {

    @Mock
    private ActionRepository actionRepository;

    private ActionService service;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-18T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

    private Document document;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ActionService(actionRepository, FIXED_CLOCK);

        document = new Document();
        document.setId(UUID.randomUUID());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Action buildAction(String title, ActionStatus status, ActionPriority priority, LocalDate deadline) {
        Action action = new Action();
        action.setId(UUID.randomUUID());
        action.setDocument(document);
        action.setTitle(title);
        action.setStatus(status);
        action.setPriority(priority);
        action.setDeadline(deadline);
        return action;
    }

    // ── getActions: ordering tests ────────────────────────────────────────

    @Test
    void getActions_pendingBeforeCompleted() {
        Action completed = buildAction("Completed", ActionStatus.COMPLETED, ActionPriority.CRITICAL, LocalDate.of(2026, 9, 20));
        Action pending = buildAction("Pending", ActionStatus.PENDING, ActionPriority.LOW, LocalDate.of(2026, 12, 31));

        when(actionRepository.findAll()).thenReturn(List.of(completed, pending));

        List<Action> result = service.getActions(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Pending");
        assertThat(result.get(1).getTitle()).isEqualTo("Completed");
    }

    @Test
    void getActions_higherPriorityFirst() {
        Action low = buildAction("Low", ActionStatus.PENDING, ActionPriority.LOW, null);
        Action critical = buildAction("Critical", ActionStatus.PENDING, ActionPriority.CRITICAL, null);
        Action medium = buildAction("Medium", ActionStatus.PENDING, ActionPriority.MEDIUM, null);
        Action high = buildAction("High", ActionStatus.PENDING, ActionPriority.HIGH, null);

        when(actionRepository.findAll()).thenReturn(List.of(low, critical, medium, high));

        List<Action> result = service.getActions(null, null);

        assertThat(result).extracting(Action::getTitle)
                .containsExactly("Critical", "High", "Medium", "Low");
    }

    @Test
    void getActions_earlierDeadlineFirst_nullsLast() {
        Action noDeadline = buildAction("No deadline", ActionStatus.PENDING, ActionPriority.HIGH, null);
        Action later = buildAction("Later", ActionStatus.PENDING, ActionPriority.HIGH, LocalDate.of(2026, 12, 1));
        Action earlier = buildAction("Earlier", ActionStatus.PENDING, ActionPriority.HIGH, LocalDate.of(2026, 10, 1));

        when(actionRepository.findAll()).thenReturn(List.of(noDeadline, later, earlier));

        List<Action> result = service.getActions(null, null);

        assertThat(result).extracting(Action::getTitle)
                .containsExactly("Earlier", "Later", "No deadline");
    }

    @Test
    void getActions_combinedSorting() {
        // Pending + Critical + early deadline should be first
        Action a1 = buildAction("A1", ActionStatus.PENDING, ActionPriority.CRITICAL, LocalDate.of(2026, 10, 1));
        // Pending + Low + late deadline
        Action a2 = buildAction("A2", ActionStatus.PENDING, ActionPriority.LOW, LocalDate.of(2026, 12, 31));
        // Completed + Critical
        Action a3 = buildAction("A3", ActionStatus.COMPLETED, ActionPriority.CRITICAL, LocalDate.of(2026, 9, 1));
        // Pending + Critical + later deadline
        Action a4 = buildAction("A4", ActionStatus.PENDING, ActionPriority.CRITICAL, LocalDate.of(2026, 11, 1));

        when(actionRepository.findAll()).thenReturn(List.of(a3, a2, a4, a1));

        List<Action> result = service.getActions(null, null);

        assertThat(result).extracting(Action::getTitle)
                .containsExactly("A1", "A4", "A2", "A3");
    }

    @Test
    void getActions_needsReviewBeforeCompleted() {
        Action needsReview = buildAction("NeedsReview", ActionStatus.NEEDS_REVIEW, ActionPriority.MEDIUM, null);
        Action completed = buildAction("Completed", ActionStatus.COMPLETED, ActionPriority.MEDIUM, null);

        when(actionRepository.findAll()).thenReturn(List.of(completed, needsReview));

        List<Action> result = service.getActions(null, null);

        assertThat(result.get(0).getTitle()).isEqualTo("NeedsReview");
        assertThat(result.get(1).getTitle()).isEqualTo("Completed");
    }

    @Test
    void getActions_emptyList() {
        when(actionRepository.findAll()).thenReturn(List.of());

        List<Action> result = service.getActions(null, null);

        assertThat(result).isEmpty();
    }

    // ── getActions: filtering tests ───────────────────────────────────────

    @Test
    void getActions_filterByStatus() {
        Action pending = buildAction("Pending", ActionStatus.PENDING, ActionPriority.MEDIUM, null);
        Action completed = buildAction("Completed", ActionStatus.COMPLETED, ActionPriority.MEDIUM, null);

        when(actionRepository.findAll()).thenReturn(List.of(pending, completed));

        List<Action> result = service.getActions(ActionStatus.PENDING, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ActionStatus.PENDING);
    }

    @Test
    void getActions_filterByPriority() {
        Action high = buildAction("High", ActionStatus.PENDING, ActionPriority.HIGH, null);
        Action low = buildAction("Low", ActionStatus.PENDING, ActionPriority.LOW, null);

        when(actionRepository.findAll()).thenReturn(List.of(high, low));

        List<Action> result = service.getActions(null, ActionPriority.HIGH);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPriority()).isEqualTo(ActionPriority.HIGH);
    }

    @Test
    void getActions_filterByBothStatusAndPriority() {
        Action match = buildAction("Match", ActionStatus.PENDING, ActionPriority.HIGH, null);
        Action wrongStatus = buildAction("WrongStatus", ActionStatus.COMPLETED, ActionPriority.HIGH, null);
        Action wrongPriority = buildAction("WrongPriority", ActionStatus.PENDING, ActionPriority.LOW, null);

        when(actionRepository.findAll()).thenReturn(List.of(match, wrongStatus, wrongPriority));

        List<Action> result = service.getActions(ActionStatus.PENDING, ActionPriority.HIGH);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Match");
    }

    // ── completeAction tests ──────────────────────────────────────────────

    @Test
    void completeAction_pendingBecomesCompleted() {
        Action pending = buildAction("Test", ActionStatus.PENDING, ActionPriority.MEDIUM, null);
        UUID actionId = pending.getId();

        when(actionRepository.findById(actionId)).thenReturn(Optional.of(pending));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        Action result = service.completeAction(actionId);

        assertThat(result.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isEqualTo(FIXED_INSTANT);
        verify(actionRepository).save(pending);
    }

    @Test
    void completeAction_alreadyCompleted_isIdempotent() {
        Instant originalCompletedAt = Instant.parse("2026-09-15T10:00:00Z");
        Action completed = buildAction("Done", ActionStatus.COMPLETED, ActionPriority.MEDIUM, null);
        completed.setCompletedAt(originalCompletedAt);
        UUID actionId = completed.getId();

        when(actionRepository.findById(actionId)).thenReturn(Optional.of(completed));

        Action result = service.completeAction(actionId);

        // Should NOT overwrite the original completedAt
        assertThat(result.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isEqualTo(originalCompletedAt);
        verify(actionRepository, never()).save(any());
    }

    @Test
    void completeAction_nonExistent_throwsActionNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(actionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeAction(nonExistentId))
                .isInstanceOf(ActionNotFoundException.class)
                .hasMessageContaining("Action not found");
    }

    @Test
    void completeAction_needsReview_becomesCompleted() {
        Action needsReview = buildAction("Review", ActionStatus.NEEDS_REVIEW, ActionPriority.MEDIUM, null);
        UUID actionId = needsReview.getId();

        when(actionRepository.findById(actionId)).thenReturn(Optional.of(needsReview));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        Action result = service.completeAction(actionId);

        assertThat(result.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isEqualTo(FIXED_INSTANT);
    }
}
