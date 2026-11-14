package com.arequiparescue.ambulance.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identificador técnico estable y ordenable de una ambulancia en el catálogo de ARE
 * (formato canónico AMB-XX, conforme a DEC-019, RN-ASI-005 y ADR-0001 Secc. 4.3.2).
 */
public record AmbulanceId(String value) implements Comparable<AmbulanceId> {

    private static final Pattern CANONICAL_AMBULANCE_ID_PATTERN = Pattern.compile("^AMB-\\d{2,}$");

    public AmbulanceId {
        Objects.requireNonNull(value, "El identificador de la ambulancia no puede ser nulo.");
        if (value.length() > 32 || !CANONICAL_AMBULANCE_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El identificador de la ambulancia debe cumplir el formato canónico AMB-XX (por ejemplo, AMB-01). Valor recibido: "
                            + value);
        }
    }

    public static AmbulanceId of(String value) {
        return new AmbulanceId(value);
    }

    public static AmbulanceId fromSequence(long sequenceNumber) {
        if (sequenceNumber < 1L) {
            throw new IllegalArgumentException("El número de secuencia de ambulancia debe ser positivo (>= 1).");
        }
        return new AmbulanceId(String.format("AMB-%02d", sequenceNumber));
    }

    @Override
    public int compareTo(AmbulanceId other) {
        Objects.requireNonNull(other, "El identificador de ambulancia a comparar no puede ser nulo.");
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
