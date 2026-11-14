package com.arequiparescue.routing.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resultado inmutable del cálculo de ruta mínima mediante algoritmo de Dijkstra sobre
 * {@link RoadGraph} (DEC-013, RN-RUT-002).
 */
public record DijkstraPathResult(
        NodeId origin,
        NodeId destination,
        int totalCost,
        List<NodeId> path
) {

    public DijkstraPathResult {
        Objects.requireNonNull(origin, "El nodo origen no puede ser nulo.");
        Objects.requireNonNull(destination, "El nodo destino no puede ser nulo.");
        if (totalCost < 0) {
            throw new IllegalArgumentException("El costo total de ruta no puede ser negativo: " + totalCost);
        }
        Objects.requireNonNull(path, "La secuencia de nodos de la ruta no puede ser nula.");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("La ruta calculada debe contener al menos el nodo origen.");
        }
        path = List.copyOf(path);
    }
}
