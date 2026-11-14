package com.arequiparescue.hospital.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identificador técnico estable y ordenable de un hospital en el catálogo de ARE
 * (formato canónico HOSP-XX, conforme a DEC-018, RN-HOS-006 y ADR-0001 Secc. 4.3.2).
 */
public record HospitalId(String value) implements Comparable<HospitalId> {

    private static final Pattern CANONICAL_HOSPITAL_ID_PATTERN = Pattern.compile("^HOSP-\\d{2,}$");

    public HospitalId {
        Objects.requireNonNull(value, "El identificador del hospital no puede ser nulo.");
        if (value.length() > 32 || !CANONICAL_HOSPITAL_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El identificador del hospital debe cumplir el formato canónico HOSP-XX (por ejemplo, HOSP-01). Valor recibido: "
                            + value);
        }
    }

    public static HospitalId of(String value) {
        return new HospitalId(value);
    }

    public static HospitalId fromSequence(long sequenceNumber) {
        if (sequenceNumber < 1L) {
            throw new IllegalArgumentException("El número de secuencia de hospital debe ser positivo (>= 1).");
        }
        return new HospitalId(String.format("HOSP-%02d", sequenceNumber));
    }

    @Override
    public int compareTo(HospitalId other) {
        Objects.requireNonNull(other, "El identificador de hospital a comparar no puede ser nulo.");
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
