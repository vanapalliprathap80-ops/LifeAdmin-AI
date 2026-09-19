package com.lifeadmin.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for the Action Center.
 * Handles action retrieval with deterministic ordering and completion workflow.
 */
@Service
public class ActionService {

    private static final Logger log = LoggerFactory.getLogger(ActionService.class);

    /**
     * Priority ordering: higher urgency = lower ordinal in this map.
     * CRITICAL(0) > HIGH(1) > MEDIUM(2) > LOW(3)
     */
    private static final Map<ActionPriority, Integer> PRIORITY_ORDER = Map.of(
            ActionPriority.CRITICAL, 0,
            ActionPriority.HIGH, 1,
            ActionPriority.MEDIUM, 2,
            ActionPriority.LOW, 3
    );

    private final ActionRepository actionRepository;
    private final Clock clock;

    public ActionService(ActionRepository actionRepository, Clock clock) {
        this.actionRepository = actionRepository;
        this.clock = clock;
    }

    /**
     * Retrieves all actions with optional filtering and deterministic ordering.
     *
     * Ordering:
     *   1. Incomplete (PENDING/NEEDS_REVIEW) before COMPLETED
     *   2. Higher priority before lower priority (CRITICAL > HIGH > MEDIUM > LOW)
     *   3. Earlier deadline before later deadline (nulls last)
     */
    @Transactional(readOnly = true)
    public List<Action> getActions(UUID userId, ActionStatus status, ActionPriority priority) {
        List<Action> actions = actionRepository.findAllByUserIdOrderByDeadlineAsc(userId);

        return actions.stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> priority == null || a.getPriority() == priority)
                .sorted(actionComparator())
                .collect(Collectors.toList());
    }

    /**
     * Marks an action as completed.
     *
     * Idempotent: if the action is already completed, returns the existing state
     * without modifying completedAt or any other field.
     *
     * @param id the action UUID
     * @param userId the user ID
     * @return the updated (or unchanged) action
     * @throws ActionNotFoundException if the action does not exist
     */
    @Transactional
    public Action completeAction(UUID id, UUID userId) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ActionNotFoundException("Action not found"));

        if (!action.getUserId().equals(userId)) {
            throw new ActionNotFoundException("Action not found");
        }

        if (action.getStatus() != ActionStatus.COMPLETED) {
            action.setStatus(ActionStatus.COMPLETED);
            action.setCompletedAt(Instant.now(clock));
            action = actionRepository.save(action);
            log.info("Action '{}' marked as completed", action.getTitle());
        } else {
            log.info("Action '{}' is already completed, returning existing state", action.getTitle());
        }

        return action;
    }

    /**
     * Builds the deterministic comparator for action ordering.
     */
    private Comparator<Action> actionComparator() {
        // 1. Incomplete before completed
        Comparator<Action> byStatus = Comparator.comparingInt(a ->
                a.getStatus() == ActionStatus.COMPLETED ? 1 : 0);

        // 2. Higher priority first (lower ordinal = higher urgency)
        Comparator<Action> byPriority = Comparator.comparingInt(a ->
                PRIORITY_ORDER.getOrDefault(a.getPriority(), 99));

        // 3. Earlier deadline first, nulls last
        Comparator<Action> byDeadline = Comparator.comparing(
                Action::getDeadline,
                Comparator.nullsLast(Comparator.naturalOrder()));

        return byStatus.thenComparing(byPriority).thenComparing(byDeadline);
    }
}
