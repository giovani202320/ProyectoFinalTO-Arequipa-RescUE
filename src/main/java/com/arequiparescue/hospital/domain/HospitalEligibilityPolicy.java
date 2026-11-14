package com.arequiparescue.hospital.domain;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.routing.domain.DijkstraPathResult;
import com.arequiparescue.routing.domain.DijkstraRouteCalculator;
import com.arequiparescue.routing.domain.RoadGraph;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Política de dominio pura para evaluar y ordenar hospitales elegibles sin especialidades clínicas:
 * habilitados ({@code enabled = true}), con plazas disponibles ({@code D = C - R - O >= 1}),
 * alcanzables por Dijkstra desde el nodo del incidente, ordenados por menor costo de ruta y
 * desempate lexicográfico ascendente por identificador (HU-09, HU-10, RN-HOS-005, RN-HOS-006, CA-HOS-01..03).
 */
public final class HospitalEligibilityPolicy {

    private final DijkstraRouteCalculator routeCalculator;

    public HospitalEligibilityPolicy() {
        this(new DijkstraRouteCalculator());
    }

    public HospitalEligibilityPolicy(DijkstraRouteCalculator routeCalculator) {
        this.routeCalculator = Objects.requireNonNull(routeCalculator);
    }

    public List<EligibleHospitalCandidate> evaluateEligibleHospitals(
            Emergency emergency,
            List<Hospital> hospitals,
            RoadGraph roadGraph
    ) {
        Objects.requireNonNull(emergency, "La emergencia es obligatoria.");
        Objects.requireNonNull(hospitals, "El catálogo de hospitales no puede ser nulo.");
        Objects.requireNonNull(roadGraph, "El grafo vial no puede ser nulo.");

        return hospitals.stream()
                .filter(Hospital::enabled)
                .filter(h -> h.availableSlots() >= 1)
                .map(h -> toCandidateIfReachable(emergency, h, roadGraph))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparingInt(EligibleHospitalCandidate::routeCost)
                        .thenComparing(c -> c.hospital().id().value()))
                .toList();
    }

    private Optional<EligibleHospitalCandidate> toCandidateIfReachable(
            Emergency emergency,
            Hospital hospital,
            RoadGraph roadGraph
    ) {
        Optional<DijkstraPathResult> path = routeCalculator.shortestPath(
                roadGraph,
                emergency.locationNode(),
                hospital.locationNode()
        );
        return path.map(p -> new EligibleHospitalCandidate(hospital, p.totalCost(), p.path()));
    }
}
