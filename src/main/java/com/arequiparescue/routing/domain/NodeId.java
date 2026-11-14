package com.arequiparescue.routing.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identificador técnico estable y ordenable de un nodo del grafo simplificado de Arequipa
 * (N01 a N10 en la topología inicial aprobada por DEC-019 y RN-RUT-006).
 */
public record NodeId(String value) implements Comparable<NodeId> {

    private static final Pattern CANONICAL_NODE_PATTERN = Pattern.compile("^N(0[1-9]|10)$");

    public NodeId {
        Objects.requireNonNull(value, "El identificador del nodo no puede ser nulo.");
        if (!CANONICAL_NODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "El identificador del nodo debe pertenecer al catálogo N01..N10. Valor recibido: " + value);
        }
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }

    @Override
    public int compareTo(NodeId other) {
        Objects.requireNonNull(other, "El identificador de nodo a comparar no puede ser nulo.");
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
