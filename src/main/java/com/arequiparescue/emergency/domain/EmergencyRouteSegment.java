package com.arequiparescue.emergency.domain;

import com.arequiparescue.routing.domain.NodeId;

import java.util.List;
import java.util.Objects;

/**
 * Objeto de valor de dominio puro que representa un segmento histórico inmutable de ruta mínima
 * calculada por Dijkstra para una emergencia (ADR-0003).
 */
public record EmergencyRouteSegment(
        EmergencyId emergencyId,
        RouteSegmentType segmentType,
        String resourceId,
        NodeId originNodeId,
        NodeId destinationNodeId,
        List<NodeId> pathNodes,
        int abstractCost,
        long calculatedAtTicks
) {

    public EmergencyRouteSegment {
        Objects.requireNonNull(emergencyId, "El identificador de la emergencia es obligatorio.");
        Objects.requireNonNull(segmentType, "El tipo de segmento de ruta es obligatorio.");
        Objects.requireNonNull(resourceId, "El identificador del recurso asociado es obligatorio.");
        if (resourceId.isBlank()) {
            throw new IllegalArgumentException("El identificador del recurso asociado no puede estar vacío.");
        }
        resourceId = resourceId.trim();
        Objects.requireNonNull(originNodeId, "El nodo origen de la ruta es obligatorio.");
        Objects.requireNonNull(destinationNodeId, "El nodo destino de la ruta es obligatorio.");
        Objects.requireNonNull(pathNodes, "La secuencia de nodos de la ruta es obligatoria.");
        if (pathNodes.isEmpty()) {
            throw new IllegalArgumentException("La secuencia de nodos de la ruta no puede estar vacía.");
        }
        pathNodes = List.copyOf(pathNodes);
        if (!pathNodes.get(0).equals(originNodeId)) {
            throw new IllegalArgumentException(
                    "El primer nodo de la ruta (" + pathNodes.get(0) + ") debe coincidir con el origen (" + originNodeId + ").");
        }
        if (!pathNodes.get(pathNodes.size() - 1).equals(destinationNodeId)) {
            throw new IllegalArgumentException(
                    "El último nodo de la ruta (" + pathNodes.get(pathNodes.size() - 1)
                            + ") debe coincidir con el destino (" + destinationNodeId + ").");
        }
        if (abstractCost < 0) {
            throw new IllegalArgumentException("El costo abstracto de la ruta no puede ser negativo: " + abstractCost);
        }
        if (calculatedAtTicks < 0) {
            throw new IllegalArgumentException("El instante lógico de cálculo no puede ser negativo: " + calculatedAtTicks);
        }
    }
}
