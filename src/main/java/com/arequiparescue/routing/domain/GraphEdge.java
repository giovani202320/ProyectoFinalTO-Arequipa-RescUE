package com.arequiparescue.routing.domain;

import java.util.Objects;

/**
 * Entidad/objeto de valor de dominio puro que representa una arista bidireccional ponderada
 * del grafo vial simplificado de Arequipa con costo entero positivo abstracto (DEC-019, RN-RUT-006).
 */
public record GraphEdge(
        String id,
        NodeId sourceNodeId,
        NodeId targetNodeId,
        int abstractCost
) {

    public GraphEdge {
        Objects.requireNonNull(id, "El identificador de la arista no puede ser nulo.");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la arista no puede estar vacío.");
        }
        Objects.requireNonNull(sourceNodeId, "El nodo origen de la arista no puede ser nulo.");
        Objects.requireNonNull(targetNodeId, "El nodo destino de la arista no puede ser nulo.");
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException(
                    "Una arista del grafo no puede conectar un nodo consigo mismo: " + sourceNodeId);
        }
        if (abstractCost <= 0) {
            throw new IllegalArgumentException(
                    "El costo abstracto de arista debe ser un entero positivo (> 0). Valor recibido: " + abstractCost);
        }
    }

    public boolean connects(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "El nodo a consultar no puede ser nulo.");
        return sourceNodeId.equals(nodeId) || targetNodeId.equals(nodeId);
    }

    public NodeId otherEndpoint(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "El nodo extremo no puede ser nulo.");
        if (sourceNodeId.equals(nodeId)) {
            return targetNodeId;
        }
        if (targetNodeId.equals(nodeId)) {
            return sourceNodeId;
        }
        throw new IllegalArgumentException("El nodo " + nodeId + " no pertenece a la arista " + id);
    }
}
