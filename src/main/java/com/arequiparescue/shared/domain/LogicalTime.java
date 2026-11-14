package com.arequiparescue.shared.domain;

import java.util.Objects;

/**
 * Objeto de valor inmutable que representa el tiempo lógico entero no negativo (t >= 0)
 * del simulador académico ARE en unidades abstractas de simulación (DEC-019, RN-RUT-009).
 */
public final class LogicalTime implements Comparable<LogicalTime> {

    public static final LogicalTime ZERO = new LogicalTime(0L);

    private final long ticks;

    private LogicalTime(long ticks) {
        if (ticks < 0L) {
            throw new IllegalArgumentException(
                    "El tiempo lógico de simulación no puede ser negativo (t >= 0). Valor recibido: " + ticks);
        }
        this.ticks = ticks;
    }

    public static LogicalTime of(long ticks) {
        return ticks == 0L ? ZERO : new LogicalTime(ticks);
    }

    public long ticks() {
        return ticks;
    }

    public LogicalTime plus(long deltaTicks) {
        if (deltaTicks < 0L) {
            throw new IllegalArgumentException(
                    "El incremento de tiempo lógico no puede ser negativo (delta >= 0). Valor recibido: " + deltaTicks);
        }
        return LogicalTime.of(Math.addExact(this.ticks, deltaTicks));
    }

    public boolean isBefore(LogicalTime other) {
        Objects.requireNonNull(other, "El instante lógico a comparar no puede ser nulo.");
        return this.ticks < other.ticks;
    }

    public boolean isAfter(LogicalTime other) {
        Objects.requireNonNull(other, "El instante lógico a comparar no puede ser nulo.");
        return this.ticks > other.ticks;
    }

    @Override
    public int compareTo(LogicalTime other) {
        Objects.requireNonNull(other, "El instante lógico a comparar no puede ser nulo.");
        return Long.compare(this.ticks, other.ticks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LogicalTime that)) {
            return false;
        }
        return ticks == that.ticks;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(ticks);
    }

    @Override
    public String toString() {
        return "t=" + ticks;
    }
}
