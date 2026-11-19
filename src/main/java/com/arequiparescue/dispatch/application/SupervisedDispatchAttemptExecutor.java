package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.AmbulanceEligibilityPolicy;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.RouteSegmentType;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.domain.port.EmergencyRouteSegmentRepository;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Ejecutor de intento atómico para asignación supervisada bajo bloqueo pesimista
 * {@code SELECT ... FOR UPDATE} en orden canónico {@code Emergency -> Ambulance}
 * conforme a {@code ADR-0002} (Decisiones 1, 3 y 7) e {@code INV-SM-08}.
 */
@Component
public class SupervisedDispatchAttemptExecutor {

    public record AttemptOutcome(
            boolean succeeded,
            boolean conflict,
            Emergency emergency,
            Ambulance ambulance,
            String reason
    ) {
    }

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final EmergencyRouteSegmentRepository routeSegmentRepository;
    private final AmbulanceEligibilityPolicy eligibilityPolicy;

    public SupervisedDispatchAttemptExecutor(
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            RoadGraphRepository roadGraphRepository,
            EmergencyRouteSegmentRepository routeSegmentRepository
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.routeSegmentRepository = Objects.requireNonNull(routeSegmentRepository);
        this.eligibilityPolicy = new AmbulanceEligibilityPolicy();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public AttemptOutcome executeSupervisedAssignmentAttempt(
            EmergencyId emergencyId,
            AmbulanceId ambulanceId,
            LogicalTime at
    ) {
        return executeAssignmentAttemptInternal(emergencyId, ambulanceId, at, "supervisada", false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public AttemptOutcome executeAutomaticAssignmentAttempt(
            EmergencyId emergencyId,
            AmbulanceId ambulanceId,
            LogicalTime at
    ) {
        return executeAssignmentAttemptInternal(emergencyId, ambulanceId, at, "automática", false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public AttemptOutcome executeQueueAssignmentAttempt(
            EmergencyId emergencyId,
            AmbulanceId ambulanceId,
            LogicalTime at
    ) {
        return executeAssignmentAttemptInternal(emergencyId, ambulanceId, at, "desde cola FIFO", true);
    }

    private AttemptOutcome executeAssignmentAttemptInternal(
            EmergencyId emergencyId,
            AmbulanceId ambulanceId,
            LogicalTime at,
            String modeLabel,
            boolean treatNonAssignableStateAsConflict
    ) {
        // Orden canónico de bloqueos ADR-0002 Decisión 7: Emergency -> Ambulance
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."
                ));

        if (emergency.state() != EmergencyState.REGISTERED
                && emergency.state() != EmergencyState.WAITING_AMBULANCE) {
            String message = "La emergencia " + emergencyId.value() + " se encuentra en estado " + emergency.state()
                    + " y no admite asignación de ambulancia.";
            if (treatNonAssignableStateAsConflict) {
                return new AttemptOutcome(false, true, emergency, null, message);
            }
            throw new DispatchValidationException(message);
        }

        Ambulance ambulance = ambulanceRepository.findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna ambulancia con identificador " + ambulanceId.value() + "."
                ));

        if (ambulance.state() != AmbulanceState.AVAILABLE || ambulance.assignedEmergencyId() != null) {
            return new AttemptOutcome(
                    false,
                    true,
                    emergency,
                    ambulance,
                    "La ambulancia " + ambulanceId.value()
                            + " dejó de estar AVAILABLE antes de confirmarse la asignación (estado actual: "
                            + ambulance.state() + ")."
            );
        }

        List<Ambulance> catalog = ambulanceRepository.findAllOrderById();
        RoadGraph graph = roadGraphRepository.loadGraph();
        List<EligibleAmbulanceCandidate> eligible =
                eligibilityPolicy.evaluateEligibleCandidates(emergency, catalog, graph);

        EligibleAmbulanceCandidate confirmedCandidate = eligible.stream()
                .filter(c -> c.ambulance().id().equals(ambulanceId))
                .findFirst()
                .orElse(null);
        if (confirmedCandidate == null) {
            return new AttemptOutcome(
                    false,
                    true,
                    emergency,
                    ambulance,
                    "La ambulancia " + ambulanceId.value()
                            + " ya no cumple las condiciones de elegibilidad/preservación vigentes al revalidar."
            );
        }

        NodeId departureNode = ambulance.currentNode();
        Emergency assignedEmergency = emergency.assignAmbulance(ambulance.id().value(), at);
        Ambulance assignedAmbulance = ambulance.assignToEmergency(emergency.id());

        Emergency savedEmergency = emergencyRepository.save(assignedEmergency);
        Ambulance savedAmbulance = ambulanceRepository.save(assignedAmbulance);

        routeSegmentRepository.saveIfAbsent(new EmergencyRouteSegment(
                savedEmergency.id(),
                RouteSegmentType.AMBULANCE_TO_INCIDENT,
                savedAmbulance.id().value(),
                departureNode,
                savedEmergency.locationNode(),
                confirmedCandidate.routePath(),
                confirmedCandidate.routeCost(),
                at.ticks()
        ));

        return new AttemptOutcome(
                true,
                false,
                savedEmergency,
                savedAmbulance,
                "Asignación " + modeLabel + " confirmada: " + savedEmergency.id().value()
                        + " -> " + savedAmbulance.id().value() + "."
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public Emergency moveEmergencyToWaitingAmbulance(
            EmergencyId emergencyId,
            LogicalTime at,
            String reason
    ) {
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."
                ));
        if (emergency.state() != EmergencyState.REGISTERED
                && emergency.state() != EmergencyState.WAITING_AMBULANCE) {
            return emergency;
        }
        Emergency waiting = emergency.enterOrRemainInWaitingAmbulance(at, reason);
        return emergencyRepository.save(waiting);
    }
}
