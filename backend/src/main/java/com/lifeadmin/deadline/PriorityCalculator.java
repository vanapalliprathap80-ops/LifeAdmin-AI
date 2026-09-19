package com.lifeadmin.deadline;

import com.lifeadmin.action.ActionPriority;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Deterministic priority calculation based on deadline proximity.
 *
 * Uses an injectable Clock for testability.
 * The LLM's confidence has NO influence on priority.
 *
 * Rules:
 *   null deadline          → MEDIUM
 *   past deadline          → CRITICAL
 *   within 7 days          → CRITICAL
 *   within 30 days         → HIGH
 *   within 90 days         → MEDIUM
 *   more than 90 days      → LOW
 */
@Component
public class PriorityCalculator {

    private final Clock clock;

    public PriorityCalculator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Calculates action priority from deadline proximity.
     *
     * @param deadline the action's deadline (may be null)
     * @return deterministic priority
     */
    public ActionPriority calculate(LocalDate deadline) {
        if (deadline == null) {
            return ActionPriority.MEDIUM;
        }

        LocalDate today = LocalDate.now(clock);
        long daysUntil = ChronoUnit.DAYS.between(today, deadline);

        if (daysUntil < 0) {
            return ActionPriority.CRITICAL; // past deadline
        }
        if (daysUntil <= 7) {
            return ActionPriority.CRITICAL;
        }
        if (daysUntil <= 30) {
            return ActionPriority.HIGH;
        }
        if (daysUntil <= 90) {
            return ActionPriority.MEDIUM;
        }
        return ActionPriority.LOW;
    }
}
