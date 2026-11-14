package com.arequiparescue.emergency.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identificador técnico estable y ordenable de una emergencia simulada (formato canónico EMG-XXXXX,
 * conforme a DEC-019 y ADR-0001 Secc. 4.3.2).
 */
public record EmergencyId(String value) implements Comparable<EmergencyId> {

    private static final Pattern CANONICAL_EMERGENCY_ID_PATTERN = Pattern.compile("^EMG-\\d{5,}$");

    public EmergencyId {
        Objects.requireNonNull(value, "El identificador de la emergencia no puede ser nulo.");
        if (value.length() > 32 || !CANONICAL_EMERGENCY_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El identificador de la emergencia debe cumplir el formato canónico EMG-XXXXX. Valor recibido: "
                            + value);
        }
    }

    public static EmergencyId of(String value) {
        return new EmergencyId(value);
    }

    public static EmergencyId fromSequence(long sequenceNumber) {
        if (sequenceNumber < 1L) {
            throw new IllegalArgumentException(
                    "El número de secuencia para generar el identificador de emergencia debe ser positivo (>= 1).");
        }
        return new EmergencyId(String.format("EMG-%05d", sequenceNumber));
    }

    @Override
    public int compareTo(EmergencyId other) {
        Objects.requireNonNull(other, "El identificador de emergencia a comparar no puede ser nulo.");
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
