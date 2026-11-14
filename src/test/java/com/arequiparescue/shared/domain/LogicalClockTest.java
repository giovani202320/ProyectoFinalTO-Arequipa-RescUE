package com.arequiparescue.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LogicalClock y LogicalTime — Invariantes de tiempo lógico monotónico (DEC-019, RN-RUT-009)")
class LogicalClockTest {

    @Test
    @DisplayName("Inicializa el reloj lógico en t = 0 por defecto")
    void shouldStartAtZeroByDefault() {
        LogicalClock clock = new LogicalClock();

        assertThat(clock.currentTime()).isEqualTo(LogicalTime.ZERO);
        assertThat(clock.currentTicks()).isZero();
    }

    @Test
    @DisplayName("Rechaza la creación de un instante lógico negativo (t < 0)")
    void shouldRejectNegativeLogicalTime() {
        assertThatThrownBy(() -> LogicalTime.of(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("t >= 0");
    }

    @Test
    @DisplayName("Permite avanzar el reloj lógico de forma monotónica no decreciente (t_nuevo >= t_actual)")
    void shouldAdvanceMonotonically() {
        LogicalClock clock = new LogicalClock();

        clock.advanceTo(LogicalTime.of(0L));
        assertThat(clock.currentTicks()).isZero();

        clock.advanceTo(LogicalTime.of(5L));
        assertThat(clock.currentTicks()).isEqualTo(5L);

        clock.advanceTo(LogicalTime.of(5L));
        assertThat(clock.currentTicks()).isEqualTo(5L);

        clock.advanceBy(7L);
        assertThat(clock.currentTicks()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Rechaza retroceder el reloj lógico a un instante anterior (t_nuevo < t_actual)")
    void shouldRejectBackwardTimeTransition() {
        LogicalClock clock = new LogicalClock(LogicalTime.of(10L));

        assertThatThrownBy(() -> clock.advanceTo(LogicalTime.of(9L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Violación de monotonicidad");

        assertThat(clock.currentTicks()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Rechaza incrementos negativos en advanceBy")
    void shouldRejectNegativeDelta() {
        LogicalClock clock = new LogicalClock(LogicalTime.of(4L));

        assertThatThrownBy(() -> clock.advanceBy(-2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delta >= 0");
    }
}
