package com.arequiparescue.dispatch.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.DijkstraPathResult;
import com.arequiparescue.routing.domain.DijkstraRouteCalculator;
import com.arequiparescue.routing.domain.RoadGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Política de dominio puro para determinar y ordenar las ambulancias elegibles de una emergencia
 * aplicando compatibilidad de equipamiento (RN-ELE-002), preservación de especializadas (RN-PRE-001..003),
 * costo mínimo por Dijkstra (RN-ELE-003, RN-RUT-002), desempate determinista por ID (RN-ASI-005, CA-SIM-02),
 * descarte de nodos inalcanzables como N10 (CA-SIM-05) e independencia hospitalaria (RN-ELE-004, RN-HOS-002).
 */
public class AmbulanceEligibilityPolicy {

    private static final Comparator<EligibleAmbulanceCandidate> DETERMINISTIC_ORDER =
            Comparator.comparingInt(EligibleAmbulanceCandidate::routeCost)
                    .thenComparing(candidate -> candidate.ambulance().id().value());

    private final DijkstraRouteCalculator routeCalculator;

    public AmbulanceEligibilityPolicy() {
        this(new DijkstraRouteCalculator());
    }

    public AmbulanceEligibilityPolicy(DijkstraRouteCalculator routeCalculator) {
        this.routeCalculator = Objects.requireNonNull(routeCalculator);
    }

    public List<EligibleAmbulanceCandidate> evaluateEligibleCandidates(
            Emergency emergency,
            List<Ambulance> catalogAmbulances,
            RoadGraph graph
    ) {
        Objects.requireNonNull(emergency, "La emergencia a evaluar no puede ser nula.");
        Objects.requireNonNull(catalogAmbulances, "El catálogo de ambulancias no puede ser nulo.");
        Objects.requireNonNull(graph, "El grafo vial no puede ser nulo.");

        if (emergency.state() != EmergencyState.REGISTERED
                && emergency.state() != EmergencyState.WAITING_AMBULANCE) {
            throw new IllegalStateException(
                    "Solo se evalúan ambulancias elegibles para emergencias en REGISTERED o WAITING_AMBULANCE. Estado actual: "
                            + emergency.state());
        }

        List<Ambulance> availableUnits = catalogAmbulances.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.state() == AmbulanceState.AVAILABLE && a.assignedEmergencyId() == null)
                .toList();

        if (emergency.requiredEquipment() == EquipmentType.SPECIALIZED) {
            return rankByEquipment(emergency, availableUnits, EquipmentType.SPECIALIZED, false, graph);
        }

        List<EligibleAmbulanceCandidate> preferredBasic =
                rankByEquipment(emergency, availableUnits, EquipmentType.BASIC, false, graph);
        if (!preferredBasic.isEmpty()) {
            return preferredBasic;
        }

        return rankByEquipment(emergency, availableUnits, EquipmentType.SPECIALIZED, true, graph);
    }

    private List<EligibleAmbulanceCandidate> rankByEquipment(
            Emergency emergency,
            List<Ambulance> availableUnits,
            EquipmentType targetEquipment,
            boolean supplementalSpecialized,
            RoadGraph graph
    ) {
        List<EligibleAmbulanceCandidate> candidates = new ArrayList<>();
        for (Ambulance ambulance : availableUnits) {
            if (ambulance.equipmentType() != targetEquipment) {
                continue;
            }
            Optional<DijkstraPathResult> pathResult = routeCalculator.shortestPath(
                    graph,
                    ambulance.currentNode(),
                    emergency.locationNode()
            );
            pathResult.ifPresent(result -> candidates.add(new EligibleAmbulanceCandidate(
                    ambulance,
                    result.totalCost(),
                    result.path(),
                    supplementalSpecialized
            )));
        }
        candidates.sort(DETERMINISTIC_ORDER);
        return List.copyOf(candidates);
    }
}
