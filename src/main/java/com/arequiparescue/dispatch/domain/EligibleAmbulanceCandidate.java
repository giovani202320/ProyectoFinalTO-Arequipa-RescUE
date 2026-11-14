package com.arequiparescue.dispatch.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.routing.domain.NodeId;

import java.util.List;
import java.util.Objects;

/**
 * Candidata elegible para despacho de una emergencia con su costo mínimo Dijkstra,
 * secuencia de nodos y marca de uso supletorio de unidad especializada (HU-05).
 */
public record EligibleAmbulanceCandidate(
        Ambulance ambulance,
        int routeCost,
        List<NodeId> routePath,
        boolean supplementalSpecialized
) {

    public EligibleAmbulanceCandidate {
        Objects.requireNonNull(ambulance, "La ambulancia candidata no puede ser nula.");
        if (routeCost < 0) {
            throw new IllegalArgumentException("El costo de ruta no puede ser negativo: " + routeCost);
        }
        Objects.requireNonNull(routePath, "La ruta calculada no puede ser nula.");
        routePath = List.copyOf(routePath);
    }
}
