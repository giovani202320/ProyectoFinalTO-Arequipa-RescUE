package com.arequiparescue.emergency.domain;

import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Objects;

/**
 * Agregado de dominio que representa una emergencia médica simulada y su estado dentro del catálogo
 * cerrado de 7 estados aprobados por DEC-019 (HU-01, HU-02, RN-EME-001 a RN-EME-011, RN-RUT-007).
 * Clase 100 % Java puro, libre de dependencias de Spring, JPA, Hibernate o Servlet.
 */
public final class Emergency {

    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final EmergencyId id;
    private final IncidentCategory category;
    private final NodeId locationNode;
    private final String description;
    private final SimulatedPriority simulatedPriority;
    private final EquipmentType requiredEquipment;
    private final TriageRuleVersionId appliedRuleVersionId;
    private final EmergencyState state;
    private final String assignedAmbulanceId;
    private final String reservedOrOccupiedHospitalId;
    private final LogicalTime registeredAt;
    private final LogicalTime queueEnteredAt;
    private final LogicalTime assignedAt;
    private final LogicalTime atIncidentAt;
    private final LogicalTime hospitalQueueEnteredAt;
    private final LogicalTime inTransportAt;
    private final LogicalTime completedAt;
    private final String lastOutcomeReason;

    private Emergency(
            EmergencyId id,
            IncidentCategory category,
            NodeId locationNode,
            String description,
            SimulatedPriority simulatedPriority,
            EquipmentType requiredEquipment,
            TriageRuleVersionId appliedRuleVersionId,
            EmergencyState state,
            String assignedAmbulanceId,
            String reservedOrOccupiedHospitalId,
            LogicalTime registeredAt,
            LogicalTime queueEnteredAt,
            LogicalTime assignedAt,
            LogicalTime atIncidentAt,
            LogicalTime hospitalQueueEnteredAt,
            LogicalTime inTransportAt,
            LogicalTime completedAt,
            String lastOutcomeReason
    ) {
        this.id = Objects.requireNonNull(id, "El identificador de la emergencia es obligatorio (RN-EME-009).");
        this.category = Objects.requireNonNull(category, "La categoría del incidente es obligatoria (RN-EME-009).");
        this.locationNode = Objects.requireNonNull(
                locationNode, "El nodo de ubicación en el grafo es obligatorio (RN-EME-009, RN-EME-010.3).");
        this.description = normalizeAndValidateDescription(description);
        this.simulatedPriority = Objects.requireNonNull(
                simulatedPriority, "La prioridad simulada informativa es obligatoria (RN-EME-009).");
        this.requiredEquipment = Objects.requireNonNull(
                requiredEquipment, "El equipamiento requerido es obligatorio (RN-EME-009).");
        this.appliedRuleVersionId = Objects.requireNonNull(
                appliedRuleVersionId, "La referencia a la versión de reglas aplicada es obligatoria (RN-EME-004, RN-EME-011).");
        this.state = Objects.requireNonNull(
                state, "El estado de la emergencia es obligatorio y debe pertenecer a los 7 estados de DEC-019.");
        this.registeredAt = Objects.requireNonNull(
                registeredAt, "La marca de tiempo lógico de registro (t >= 0) es obligatoria (RN-EME-009).");
        this.assignedAmbulanceId = normalizeNullableIdentifier(assignedAmbulanceId);
        this.reservedOrOccupiedHospitalId = normalizeNullableIdentifier(reservedOrOccupiedHospitalId);
        this.queueEnteredAt = queueEnteredAt;
        this.assignedAt = assignedAt;
        this.atIncidentAt = atIncidentAt;
        this.hospitalQueueEnteredAt = hospitalQueueEnteredAt;
        this.inTransportAt = inTransportAt;
        this.completedAt = completedAt;
        this.lastOutcomeReason = normalizeNullableIdentifier(lastOutcomeReason);

        validateStateResourceInvariants(this.state, this.assignedAmbulanceId, this.reservedOrOccupiedHospitalId);
    }

    /**
     * Registra una nueva emergencia simulada en su estado inicial obligatorio {@link EmergencyState#REGISTERED}
     * (transición T-EME-00 de la máquina de estados aprobada, HU-01, RN-EME-009, RN-EME-010).
     */
    public static Emergency register(
            EmergencyId id,
            NodeId locationNode,
            String description,
            TriageClassification classification,
            LogicalTime registeredAt
    ) {
        Objects.requireNonNull(
                classification,
                "La clasificación determinista de triage es obligatoria para registrar la emergencia (RN-EME-010.5)."
        );
        return new Emergency(
                id,
                classification.category(),
                locationNode,
                description,
                classification.simulatedPriority(),
                classification.requiredEquipment(),
                classification.appliedRuleVersionId(),
                EmergencyState.REGISTERED,
                null,
                null,
                registeredAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Reconstruye la entidad de dominio desde su representación persistente preservando todas las invariantes.
     */
    public static Emergency reconstruct(
            EmergencyId id,
            IncidentCategory category,
            NodeId locationNode,
            String description,
            SimulatedPriority simulatedPriority,
            EquipmentType requiredEquipment,
            TriageRuleVersionId appliedRuleVersionId,
            EmergencyState state,
            String assignedAmbulanceId,
            String reservedOrOccupiedHospitalId,
            LogicalTime registeredAt,
            LogicalTime queueEnteredAt,
            LogicalTime assignedAt,
            LogicalTime atIncidentAt,
            LogicalTime hospitalQueueEnteredAt,
            LogicalTime inTransportAt,
            LogicalTime completedAt,
            String lastOutcomeReason
    ) {
        return new Emergency(
                id,
                category,
                locationNode,
                description,
                simulatedPriority,
                requiredEquipment,
                appliedRuleVersionId,
                state,
                assignedAmbulanceId,
                reservedOrOccupiedHospitalId,
                registeredAt,
                queueEnteredAt,
                assignedAt,
                atIncidentAt,
                hospitalQueueEnteredAt,
                inTransportAt,
                completedAt,
                lastOutcomeReason
        );
    }

    private static String normalizeAndValidateDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return "";
        }
        String trimmed = rawDescription.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "La descripción ficticia no puede superar los " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private static String normalizeNullableIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private static void validateStateResourceInvariants(
            EmergencyState state,
            String assignedAmbulanceId,
            String reservedOrOccupiedHospitalId
    ) {
        switch (state) {
            case REGISTERED, WAITING_AMBULANCE -> {
                if (assignedAmbulanceId != null) {
                    throw new IllegalArgumentException(
                            "Una emergencia en estado " + state + " no puede tener una ambulancia activamente vinculada (INV-SM-03).");
                }
            }
            case ASSIGNED, AT_INCIDENT, WAITING_HOSPITAL -> {
                if (assignedAmbulanceId == null) {
                    throw new IllegalArgumentException(
                            "Una emergencia en estado " + state + " requiere conservar su ambulancia asignada (INV-SM-03).");
                }
            }
            case IN_TRANSPORT -> {
                if (assignedAmbulanceId == null || reservedOrOccupiedHospitalId == null) {
                    throw new IllegalArgumentException(
                            "Una emergencia en estado IN_TRANSPORT requiere ambulancia asignada y hospital reservado (INV-SM-03, INV-SM-04).");
                }
            }
            case COMPLETED -> {
                if (reservedOrOccupiedHospitalId == null) {
                    throw new IllegalArgumentException(
                            "Una emergencia en estado COMPLETED requiere conservar la referencia al hospital de entrega (RN-EME-001, RN-RUT-005).");
                }
            }
        }
    }

    public EmergencyId id() {
        return id;
    }

    public IncidentCategory category() {
        return category;
    }

    public NodeId locationNode() {
        return locationNode;
    }

    public String description() {
        return description;
    }

    public SimulatedPriority simulatedPriority() {
        return simulatedPriority;
    }

    public EquipmentType requiredEquipment() {
        return requiredEquipment;
    }

    public TriageRuleVersionId appliedRuleVersionId() {
        return appliedRuleVersionId;
    }

    public EmergencyState state() {
        return state;
    }

    public String assignedAmbulanceId() {
        return assignedAmbulanceId;
    }

    public String reservedOrOccupiedHospitalId() {
        return reservedOrOccupiedHospitalId;
    }

    public LogicalTime registeredAt() {
        return registeredAt;
    }

    public LogicalTime queueEnteredAt() {
        return queueEnteredAt;
    }

    public LogicalTime assignedAt() {
        return assignedAt;
    }

    public LogicalTime atIncidentAt() {
        return atIncidentAt;
    }

    public LogicalTime hospitalQueueEnteredAt() {
        return hospitalQueueEnteredAt;
    }

    public LogicalTime inTransportAt() {
        return inTransportAt;
    }

    public LogicalTime completedAt() {
        return completedAt;
    }

    public String lastOutcomeReason() {
        return lastOutcomeReason;
    }

    /**
     * Indica si la emergencia se encuentra esperando destino hospitalario con su ambulancia
     * retenida en estado AT_INCIDENT, diferenciándose de WAITING_AMBULANCE (HU-02 CA-2, RN-RUT-007).
     */
    public boolean retainsAmbulanceAtIncidentWaitingHospital() {
        return this.state == EmergencyState.WAITING_HOSPITAL && this.assignedAmbulanceId != null;
    }

    /**
     * Indica si el ciclo prehospitalario simulado de la emergencia ha concluido en el hospital
     * sin etapas intrahospitalarias posteriores (HU-02 CA-3, RN-EME-001, RN-RUT-005).
     */
    /**
     * Ejecuta T-EME-01 (REGISTERED -> ASSIGNED) o T-EME-03 (WAITING_AMBULANCE -> ASSIGNED)
     * vinculando la ambulancia seleccionada y sellando assignedAt (HU-06 CA-1, INV-SM-03).
     */
    public Emergency assignAmbulance(String ambulanceId, LogicalTime at) {
        Objects.requireNonNull(at, "El instante lógico de asignación no puede ser nulo.");
        if (ambulanceId == null || ambulanceId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la ambulancia asignada es obligatorio.");
        }
        if (this.state != EmergencyState.REGISTERED && this.state != EmergencyState.WAITING_AMBULANCE) {
            throw new IllegalStateException(
                    "Solo una emergencia en estado REGISTERED o WAITING_AMBULANCE puede recibir asignación de ambulancia. Estado actual: "
                            + this.state);
        }
        if (this.assignedAmbulanceId != null) {
            throw new IllegalStateException(
                    "La emergencia " + this.id + " ya tiene una ambulancia vinculada: " + this.assignedAmbulanceId);
        }
        return new Emergency(
                this.id,
                this.category,
                this.locationNode,
                this.description,
                this.simulatedPriority,
                this.requiredEquipment,
                this.appliedRuleVersionId,
                EmergencyState.ASSIGNED,
                ambulanceId.trim(),
                this.reservedOrOccupiedHospitalId,
                this.registeredAt,
                this.queueEnteredAt,
                at,
                this.atIncidentAt,
                this.hospitalQueueEnteredAt,
                this.inTransportAt,
                this.completedAt,
                null
        );
    }

    /**
     * Ejecuta T-EME-02 (REGISTERED -> WAITING_AMBULANCE) o conserva WAITING_AMBULANCE sin alterar
     * su marca FIFO original cuando no quedan ambulancias elegibles disponibles (RN-ASI-004, HU-06 CA-3).
     */
    public Emergency enterOrRemainInWaitingAmbulance(LogicalTime at, String outcomeReason) {
        Objects.requireNonNull(at, "El instante lógico de ingreso a cola no puede ser nulo.");
        if (this.state == EmergencyState.WAITING_AMBULANCE) {
            return new Emergency(
                    this.id,
                    this.category,
                    this.locationNode,
                    this.description,
                    this.simulatedPriority,
                    this.requiredEquipment,
                    this.appliedRuleVersionId,
                    EmergencyState.WAITING_AMBULANCE,
                    null,
                    this.reservedOrOccupiedHospitalId,
                    this.registeredAt,
                    this.queueEnteredAt,
                    this.assignedAt,
                    this.atIncidentAt,
                    this.hospitalQueueEnteredAt,
                    this.inTransportAt,
                    this.completedAt,
                    outcomeReason
            );
        }
        if (this.state != EmergencyState.REGISTERED) {
            throw new IllegalStateException(
                    "Solo una emergencia en estado REGISTERED o WAITING_AMBULANCE puede pasar o permanecer en espera de ambulancia. Estado actual: "
                            + this.state);
        }
        return new Emergency(
                this.id,
                this.category,
                this.locationNode,
                this.description,
                this.simulatedPriority,
                this.requiredEquipment,
                this.appliedRuleVersionId,
                EmergencyState.WAITING_AMBULANCE,
                null,
                this.reservedOrOccupiedHospitalId,
                this.registeredAt,
                at,
                this.assignedAt,
                this.atIncidentAt,
                this.hospitalQueueEnteredAt,
                this.inTransportAt,
                this.completedAt,
                outcomeReason
        );
    }

    public boolean isCycleClosed() {
        return this.state == EmergencyState.COMPLETED;
    }
}
