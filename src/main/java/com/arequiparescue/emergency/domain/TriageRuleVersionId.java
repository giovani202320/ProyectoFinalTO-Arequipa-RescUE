package com.arequiparescue.emergency.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identificador técnico estable e inmutable de una versión del catálogo de reglas de clasificación
 * (formato canónico RUL-VXX, conforme a DEC-016, RN-EME-011 y ADR-0001 Secc. 4.3.2).
 */
public record TriageRuleVersionId(String value) implements Comparable<TriageRuleVersionId> {

    private static final Pattern CANONICAL_VERSION_ID_PATTERN = Pattern.compile("^RUL-V\\d{2,}$");

    public TriageRuleVersionId {
        Objects.requireNonNull(value, "El identificador de la versión de reglas no puede ser nulo.");
        if (value.length() > 32 || !CANONICAL_VERSION_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El identificador de la versión de reglas debe cumplir el formato canónico RUL-VXX. Valor recibido: "
                            + value);
        }
    }

    public static TriageRuleVersionId of(String value) {
        return new TriageRuleVersionId(value);
    }

    @Override
    public int compareTo(TriageRuleVersionId other) {
        Objects.requireNonNull(other, "El identificador de versión de reglas a comparar no puede ser nulo.");
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
