package com.lifeadmin.deadline;

import com.lifeadmin.action.ActionPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityCalculatorTest {

    private PriorityCalculator calculator;
    private final LocalDate today = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setUp() {
        // Fix the clock to a specific date for deterministic testing
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-09-18T12:00:00Z"),
                ZoneId.of("UTC")
        );
        calculator = new PriorityCalculator(fixedClock);
    }

    @Test
    void testNullDeadline_defaultsToMedium() {
        assertThat(calculator.calculate(null)).isEqualTo(ActionPriority.MEDIUM);
    }

    @Test
    void testPastDeadline_isCritical() {
        LocalDate yesterday = today.minusDays(1);
        assertThat(calculator.calculate(yesterday)).isEqualTo(ActionPriority.CRITICAL);
    }

    @Test
    void testTodayDeadline_isCritical() {
        assertThat(calculator.calculate(today)).isEqualTo(ActionPriority.CRITICAL);
    }

    @Test
    void testExactly7DaysAway_isCritical() {
        LocalDate deadline = today.plusDays(7);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.CRITICAL);
    }

    @Test
    void testExactly8DaysAway_isHigh() {
        LocalDate deadline = today.plusDays(8);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.HIGH);
    }

    @Test
    void testExactly30DaysAway_isHigh() {
        LocalDate deadline = today.plusDays(30);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.HIGH);
    }

    @Test
    void testExactly31DaysAway_isMedium() {
        LocalDate deadline = today.plusDays(31);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.MEDIUM);
    }

    @Test
    void testExactly90DaysAway_isMedium() {
        LocalDate deadline = today.plusDays(90);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.MEDIUM);
    }

    @Test
    void testExactly91DaysAway_isLow() {
        LocalDate deadline = today.plusDays(91);
        assertThat(calculator.calculate(deadline)).isEqualTo(ActionPriority.LOW);
    }
}
