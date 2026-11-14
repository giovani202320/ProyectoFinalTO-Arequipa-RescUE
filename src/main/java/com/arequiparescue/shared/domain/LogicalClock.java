package com.arequiparescue.shared.domain;

import java.util.Objects;

/**
 * Primitiva de reloj lógico monotónico no negativo (t >= 0) del simulador ARE.
 * Garantiza que el tiempo simulado nunca retroceda (DEC-019, RN-RUT-009, CA-SIM-06).
 */
public final class LogicalClock {

    private LogicalTime currentTime;

    public LogicalClock() {
        this(LogicalTime.ZERO);
    }

    public LogicalClock(LogicalTime initialTime) {
        this.currentTime = Objects.requireNonNull(initialTime, "El tiempo lógico inicial no puede ser nulo.");
    }

    public LogicalTime currentTime() {
        return currentTime;
    }

    public long currentTicks() {
        return currentTime.ticks();
    }

    /**
     * Avanza de forma monotónica el reloj lógico hacia {@code targetTime}.
     * Rechaza cualquier intento de retroceder el tiempo lógico (targetTime &lt; currentTime).
     */
    public LogicalTime advanceTo(LogicalTime targetTime) {
        Objects.requireNonNull(targetTime, "El instante lógico objetivo no puede ser nulo.");
        if (targetTime.isBefore(this.currentTime)) {
            throw new IllegalArgumentException(
                    "Violación de monotonicidad del reloj lógico: no se permite retroceder de "
                            + this.currentTime + " a " + targetTime + " (RN-RUT-009).");
        }
        this.currentTime = targetTime;
        return this.currentTime;
    }

    /**
     * Avanza el reloj lógico sumando {@code deltaTicks >= 0} al instante actual.
     */
    public LogicalTime advanceBy(long deltaTicks) {
        return advanceTo(this.currentTime.plus(deltaTicks));
    }
}
