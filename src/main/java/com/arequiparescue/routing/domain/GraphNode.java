package com.arequiparescue.routing.domain;

import java.util.Objects;

/**
 * Entidad/objeto de valor de dominio puro que representa un nodo de la red vial
 * simplificada de Arequipa (DEC-013, DEC-019, RN-RUT-006).
 */
public record GraphNode(
        NodeId id,
        String zoneCode,
        String displayName,
        boolean isolatedInBaseTopology
) {

    public GraphNode {
        Objects.requireNonNull(id, "El identificador del nodo no puede ser nulo.");
        Objects.requireNonNull(zoneCode, "El código de zona del nodo no puede ser nulo.");
        if (zoneCode.isBlank()) {
            throw new IllegalArgumentException("El código de zona del nodo no puede estar vacío.");
        }
        Objects.requireNonNull(displayName, "El nombre descriptivo del nodo no puede ser nulo.");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("El nombre descriptivo del nodo no puede estar vacío.");
        }
    }
}
