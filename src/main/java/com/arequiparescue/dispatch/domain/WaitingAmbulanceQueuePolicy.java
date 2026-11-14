package com.arequiparescue.dispatch.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.DijkstraRouteCalculator;
import com.arequiparescue.routing.domain.RoadGraph;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Política de dominio pura para seleccionar la siguiente emergencia en {@code WAITING_AMBULANCE}
 * al liberarse o estar disponible una ambulancia, respetando equipamiento, preservación y FIFO
 * estricto sin aging ni prioridad clínica (HU-08, DEC-012, RN-COL-001 a RN-COL-007, CA-SIM-01).
 */
public final class WaitingAmbulanceQueuePolicy {

    private static final Comparator<Emergency> FIFO_COMPARATOR = Comparator
            .comparingLong((Emergency e) -> e.queueEnteredAt() != null
                    ? e.queueEnteredAt().ticks()
                    : e.registeredAt().ticks())
            .thenComparing(e -> e.id().value());

    private final DijkstraRouteCalculator routeCalculator;

    public WaitingAmbulanceQueuePolicy() {
        this(new DijkstraRouteCalculator());
    }

    public WaitingAmbulanceQueuePolicy(DijkstraRouteCalculator routeCalculator) {
        this.routeCalculator = Objects.requireNonNull(routeCalculator);
    }

    /**
     * Ordena normativamente la cola {@code WAITING_AMBULANCE} por {@code queue_entered_at_t ASC, id ASC}.
     */
    public List<Emergency> sortFifo(List<Emergency> waitingEmergencies) {
        Objects.requireNonNull(waitingEmergencies, "La lista de emergencias en espera no puede ser nula.");
        return waitingEmergencies.stream()
                .filter(e -> e.state() == EmergencyState.WAITING_AMBULANCE)
                .sorted(FIFO_COMPARATOR)
                .toList();
    }

    /**
     * Evalúa qué emergencia en {@code WAITING_AMBULANCE} debe atenderse para una ambulancia {@code AVAILABLE}.
     */
    public Optional<Emergency> selectNextEmergencyForAvailableAmbulance(
            Ambulance availableAmbulance,
            List<Emergency> waitingEmergencies,
            RoadGraph roadGraph
    ) {
        Objects.requireNonNull(availableAmbulance, "La ambulancia a evaluar no puede ser nula.");
        Objects.requireNonNull(waitingEmergencies, "La cola de emergencias en espera no puede ser nula.");
        Objects.requireNonNull(roadGraph, "El grafo vial no puede ser nulo.");

        if (availableAmbulance.state() != AmbulanceState.AVAILABLE
                || availableAmbulance.assignedEmergencyId() != null) {
            return Optional.empty();
        }

        List<Emergency> fifoQueue = sortFifo(waitingEmergencies);
        if (fifoQueue.isEmpty()) {
            return Optional.empty();
        }

        if (availableAmbulance.equipmentType() == EquipmentType.SPECIALIZED) {
            Optional<Emergency> specializedMatch = findFirstReachableInGroup(
                    availableAmbulance, fifoQueue, EquipmentType.SPECIALIZED, roadGraph);
            if (specializedMatch.isPresent()) {
                return specializedMatch;
            }
            return findFirstReachableInGroup(
                    availableAmbulance, fifoQueue, EquipmentType.BASIC, roadGraph);
        }

        return findFirstReachableInGroup(
                availableAmbulance, fifoQueue, EquipmentType.BASIC, roadGraph);
    }

    private Optional<Emergency> findFirstReachableInGroup(
            Ambulance ambulance,
            List<Emergency> fifoQueue,
            EquipmentType targetEquipment,
            RoadGraph roadGraph
    ) {
        for (Emergency emergency : fifoQueue) {
            if (emergency.requiredEquipment() != targetEquipment) {
                continue;
            }
            if (routeCalculator.shortestPath(roadGraph, ambulance.currentNode(), emergency.locationNode()).isPresent()) {
                return Optional.of(emergency);
            }
        }
        return Optional.empty();
    }
}
