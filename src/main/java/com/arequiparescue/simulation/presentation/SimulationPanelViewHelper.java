package com.arequiparescue.simulation.presentation;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Componente de presentación de solo lectura que proyecta el estado operativo actual de emergencias,
 * ambulancias, hospitales y reservas hacia {@code /simulation} y construye comparaciones reales
 * antes/después al avanzar un evento discreto (P1-1), sin alterar el dominio ni la persistencia.
 */
@Component
public class SimulationPanelViewHelper {

    static final int MAX_VISIBLE_ACTIVE_EMERGENCIES = 8;

    private final EmergencyApplicationService emergencyService;
    private final AmbulanceCatalogApplicationService ambulanceService;
    private final HospitalCatalogApplicationService hospitalService;
    private final HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeService;

    public SimulationPanelViewHelper(
            EmergencyApplicationService emergencyService,
            AmbulanceCatalogApplicationService ambulanceService,
            HospitalCatalogApplicationService hospitalService,
            HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeService
    ) {
        this.emergencyService = Objects.requireNonNull(emergencyService);
        this.ambulanceService = Objects.requireNonNull(ambulanceService);
        this.hospitalService = Objects.requireNonNull(hospitalService);
        this.deliveryAndDischargeService = Objects.requireNonNull(deliveryAndDischargeService);
    }

    public RawOperationalState captureRawState() {
        List<Emergency> emergencies = emergencyService.listEmergencies();
        List<Ambulance> ambulances = ambulanceService.listAmbulances();
        List<Hospital> hospitals = hospitalService.listHospitals();
        List<HospitalReservation> reservations = deliveryAndDischargeService.listHospitalReservations();
        return new RawOperationalState(emergencies, ambulances, hospitals, reservations);
    }

    public OperationalSnapshot buildOperationalSnapshot() {
        return toOperationalSnapshot(captureRawState());
    }

    public static OperationalSnapshot toOperationalSnapshot(RawOperationalState raw) {
        List<ActiveEmergencyItem> allActive = new ArrayList<>();
        long completedCount = 0L;
        for (Emergency e : raw.emergencies()) {
            if (e.state() == EmergencyState.COMPLETED) {
                completedCount++;
            } else {
                allActive.add(new ActiveEmergencyItem(
                        e.id().value(),
                        e.state().name(),
                        e.locationNode().value(),
                        e.requiredEquipment().name(),
                        e.assignedAmbulanceId(),
                        e.reservedOrOccupiedHospitalId()
                ));
            }
        }

        List<ActiveEmergencyItem> visibleActive = allActive.size() > MAX_VISIBLE_ACTIVE_EMERGENCIES
                ? List.copyOf(allActive.subList(0, MAX_VISIBLE_ACTIVE_EMERGENCIES))
                : List.copyOf(allActive);

        List<AmbulanceItem> ambulanceItems = new ArrayList<>();
        long availableAmbulances = 0L;
        for (Ambulance a : raw.ambulances()) {
            if (a.state() == AmbulanceState.AVAILABLE) {
                availableAmbulances++;
            }
            ambulanceItems.add(new AmbulanceItem(
                    a.id().value(),
                    a.equipmentType().name(),
                    a.currentNode().value(),
                    a.state().name(),
                    a.assignedEmergencyId() != null ? a.assignedEmergencyId().value() : null
            ));
        }

        List<HospitalCapacityItem> hospitalItems = new ArrayList<>();
        int totalCapacity = 0;
        int totalAvailable = 0;
        for (Hospital h : raw.hospitals()) {
            totalCapacity += h.totalCapacity();
            if (h.enabled()) {
                totalAvailable += h.availableSlots();
            }
            hospitalItems.add(new HospitalCapacityItem(
                    h.id().value(),
                    h.name(),
                    h.locationNode().value(),
                    h.enabled(),
                    h.totalCapacity(),
                    h.reservedSlots(),
                    h.occupiedSlots(),
                    h.availableSlots()
            ));
        }

        List<ActiveReservationItem> activeReservations = raw.reservations().stream()
                .filter(r -> r.status() == HospitalReservationStatus.RESERVED
                        || r.status() == HospitalReservationStatus.OCCUPIED)
                .map(r -> new ActiveReservationItem(
                        r.id(),
                        r.emergencyId().value(),
                        r.hospitalId().value(),
                        r.status().name()
                ))
                .toList();

        List<SubjectSuggestion> suggestions = buildSubjectSuggestions(raw);

        return new OperationalSnapshot(
                visibleActive,
                allActive.size(),
                completedCount,
                List.copyOf(ambulanceItems),
                availableAmbulances,
                ambulanceItems.size(),
                List.copyOf(hospitalItems),
                totalAvailable,
                totalCapacity,
                activeReservations,
                suggestions
        );
    }

    public static StepComparisonView buildStepComparison(
            RawOperationalState before,
            RawOperationalState after,
            SimulationStepResult stepResult
    ) {
        ScheduledSimulationEvent executed = stepResult.processedEvent();
        String eventId = executed != null ? executed.event().eventId() : "—";
        String eventTypeName = executed != null ? executed.event().type().name() : null;
        String subjectId = executed != null ? executed.event().subjectId() : "—";

        List<ResourceDelta> deltas = computeDeltas(before, after);
        return new StepComparisonView(
                eventId,
                eventTypeName,
                subjectId,
                stepResult.previousTime().ticks(),
                stepResult.currentTime().ticks(),
                stepResult.outcome().name(),
                stepResult.message(),
                deltas
        );
    }

    private static List<ResourceDelta> computeDeltas(RawOperationalState before, RawOperationalState after) {
        List<ResourceDelta> deltas = new ArrayList<>();

        Map<String, Emergency> emgBefore = new LinkedHashMap<>();
        before.emergencies().forEach(e -> emgBefore.put(e.id().value(), e));
        for (Emergency eAfter : after.emergencies()) {
            Emergency eBefore = emgBefore.get(eAfter.id().value());
            String afterSummary = describeEmergency(eAfter);
            if (eBefore == null) {
                deltas.add(new ResourceDelta(eAfter.id().value(), "Emergencia", "No registrada", afterSummary));
            } else {
                String beforeSummary = describeEmergency(eBefore);
                if (!beforeSummary.equals(afterSummary)) {
                    deltas.add(new ResourceDelta(eAfter.id().value(), "Emergencia", beforeSummary, afterSummary));
                }
            }
        }

        Map<String, Ambulance> ambBefore = new LinkedHashMap<>();
        before.ambulances().forEach(a -> ambBefore.put(a.id().value(), a));
        for (Ambulance aAfter : after.ambulances()) {
            Ambulance aBefore = ambBefore.get(aAfter.id().value());
            String afterSummary = describeAmbulance(aAfter);
            if (aBefore == null) {
                deltas.add(new ResourceDelta(aAfter.id().value(), "Ambulancia", "No registrada", afterSummary));
            } else {
                String beforeSummary = describeAmbulance(aBefore);
                if (!beforeSummary.equals(afterSummary)) {
                    deltas.add(new ResourceDelta(aAfter.id().value(), "Ambulancia", beforeSummary, afterSummary));
                }
            }
        }

        Map<String, Hospital> hospBefore = new LinkedHashMap<>();
        before.hospitals().forEach(h -> hospBefore.put(h.id().value(), h));
        for (Hospital hAfter : after.hospitals()) {
            Hospital hBefore = hospBefore.get(hAfter.id().value());
            String afterSummary = describeHospital(hAfter);
            if (hBefore == null) {
                deltas.add(new ResourceDelta(hAfter.id().value(), "Hospital", "No registrado", afterSummary));
            } else {
                String beforeSummary = describeHospital(hBefore);
                if (!beforeSummary.equals(afterSummary)) {
                    deltas.add(new ResourceDelta(hAfter.id().value(), "Hospital", beforeSummary, afterSummary));
                }
            }
        }

        Map<String, HospitalReservation> resBefore = new LinkedHashMap<>();
        before.reservations().forEach(r -> resBefore.put(r.id(), r));
        for (HospitalReservation rAfter : after.reservations()) {
            HospitalReservation rBefore = resBefore.get(rAfter.id());
            String afterSummary = rAfter.status().name() + " (" + rAfter.hospitalId().value() + ")";
            if (rBefore == null) {
                deltas.add(new ResourceDelta(rAfter.id(), "Reserva", "Sin reserva", afterSummary));
            } else {
                String beforeSummary = rBefore.status().name() + " (" + rBefore.hospitalId().value() + ")";
                if (!beforeSummary.equals(afterSummary)) {
                    deltas.add(new ResourceDelta(rAfter.id(), "Reserva", beforeSummary, afterSummary));
                }
            }
        }

        return List.copyOf(deltas);
    }

    private static String describeEmergency(Emergency e) {
        StringBuilder sb = new StringBuilder(e.state().name());
        if (e.assignedAmbulanceId() != null) {
            sb.append(" · ").append(e.assignedAmbulanceId());
        }
        if (e.reservedOrOccupiedHospitalId() != null) {
            sb.append(" · ").append(e.reservedOrOccupiedHospitalId());
        }
        return sb.toString();
    }

    private static String describeAmbulance(Ambulance a) {
        StringBuilder sb = new StringBuilder(a.state().name())
                .append(" en ").append(a.currentNode().value());
        if (a.assignedEmergencyId() != null) {
            sb.append(" (").append(a.assignedEmergencyId().value()).append(")");
        }
        return sb.toString();
    }

    private static String describeHospital(Hospital h) {
        return "R=" + h.reservedSlots() + ", O=" + h.occupiedSlots() + ", D=" + h.availableSlots();
    }

    private static List<SubjectSuggestion> buildSubjectSuggestions(RawOperationalState raw) {
        List<SubjectSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new SubjectSuggestion("SYS-QUEUE", "Evaluación de colas de espera (WAITING_QUEUE_EVALUATION)"));

        for (Emergency e : raw.emergencies()) {
            if (e.state() == EmergencyState.ASSIGNED) {
                suggestions.add(new SubjectSuggestion(
                        e.id().value(),
                        "Llegada al incidente (AMBULANCE_ARRIVAL_AT_INCIDENT) — nodo " + e.locationNode().value()));
            } else if (e.state() == EmergencyState.IN_TRANSPORT) {
                suggestions.add(new SubjectSuggestion(
                        e.id().value(),
                        "Entrega hospitalaria (RESOURCE_RELEASE) — hacia " + e.reservedOrOccupiedHospitalId()));
            } else if (e.state() != EmergencyState.COMPLETED) {
                suggestions.add(new SubjectSuggestion(
                        e.id().value(),
                        "Emergencia " + e.state().name() + " — nodo " + e.locationNode().value()));
            }
        }

        for (HospitalReservation r : raw.reservations()) {
            if (r.status() == HospitalReservationStatus.OCCUPIED) {
                suggestions.add(new SubjectSuggestion(
                        r.id(),
                        "Alta hospitalaria simulada (RESOURCE_RELEASE) — " + r.hospitalId().value()));
            }
        }

        for (Ambulance a : raw.ambulances()) {
            if (a.state() == AmbulanceState.AVAILABLE) {
                suggestions.add(new SubjectSuggestion(
                        a.id().value(),
                        "Liberación / reevaluación ambulancia (RESOURCE_RELEASE) — " + a.currentNode().value()));
            }
        }

        for (Hospital h : raw.hospitals()) {
            if (h.enabled()) {
                suggestions.add(new SubjectSuggestion(
                        h.id().value(),
                        "Reevaluación cola hospitalaria (RESOURCE_RELEASE) — D=" + h.availableSlots()));
            }
        }

        return List.copyOf(suggestions);
    }

    public record RawOperationalState(
            List<Emergency> emergencies,
            List<Ambulance> ambulances,
            List<Hospital> hospitals,
            List<HospitalReservation> reservations
    ) {
        public static RawOperationalState empty() {
            return new RawOperationalState(List.of(), List.of(), List.of(), List.of());
        }
    }

    public record OperationalSnapshot(
            List<ActiveEmergencyItem> activeEmergencies,
            int totalActiveEmergencies,
            long totalCompletedEmergencies,
            List<AmbulanceItem> ambulances,
            long availableAmbulancesCount,
            int totalAmbulancesCount,
            List<HospitalCapacityItem> hospitals,
            int totalAvailableBeds,
            int totalCapacityBeds,
            List<ActiveReservationItem> activeReservations,
            List<SubjectSuggestion> subjectSuggestions
    ) {
        public static OperationalSnapshot empty() {
            return new OperationalSnapshot(
                    List.of(), 0, 0L, List.of(), 0L, 0, List.of(), 0, 0, List.of(),
                    List.of(new SubjectSuggestion("SYS-QUEUE", "Evaluación de colas de espera"))
            );
        }
    }

    public record ActiveEmergencyItem(
            String id,
            String state,
            String nodeId,
            String requiredEquipment,
            String assignedAmbulanceId,
            String hospitalId
    ) {
    }

    public record AmbulanceItem(
            String id,
            String equipmentType,
            String currentNodeId,
            String state,
            String assignedEmergencyId
    ) {
    }

    public record HospitalCapacityItem(
            String id,
            String name,
            String nodeId,
            boolean enabled,
            int totalCapacity,
            int reservedSlots,
            int occupiedSlots,
            int availableSlots
    ) {
    }

    public record ActiveReservationItem(
            String id,
            String emergencyId,
            String hospitalId,
            String status
    ) {
    }

    public record SubjectSuggestion(
            String value,
            String label
    ) {
    }

    public record StepComparisonView(
            String eventId,
            String eventTypeName,
            String subjectId,
            long previousTicks,
            long currentTicks,
            String outcome,
            String message,
            List<ResourceDelta> deltas
    ) {
        public boolean hasOperationalChanges() {
            return deltas != null && !deltas.isEmpty();
        }
    }

    public record ResourceDelta(
            String resourceId,
            String resourceKind,
            String beforeState,
            String afterState
    ) {
    }
}
