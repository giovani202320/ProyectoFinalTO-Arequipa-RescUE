package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code emergencies} (DEC-003, DEC-016, DEC-019, ADR-0001 Secc. 4.3).
 * No se expone directamente a los controladores ni a las vistas Thymeleaf.
 */
@Entity
@Table(name = "emergencies")
public class EmergencyJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "incident_category", length = 32, nullable = false)
    private String incidentCategory;

    @Column(name = "node_id", length = 32, nullable = false)
    private String nodeId;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "simulated_priority", length = 16, nullable = false)
    private String simulatedPriority;

    @Column(name = "required_equipment", length = 16, nullable = false)
    private String requiredEquipment;

    @Column(name = "rule_version_id", length = 32, nullable = false)
    private String ruleVersionId;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "assigned_ambulance_id", length = 32)
    private String assignedAmbulanceId;

    @Column(name = "hospital_id", length = 32)
    private String hospitalId;

    @Column(name = "registered_at_t", nullable = false)
    private long registeredAtTicks;

    @Column(name = "queue_entered_at_t")
    private Long queueEnteredAtTicks;

    @Column(name = "assigned_at_t")
    private Long assignedAtTicks;

    @Column(name = "at_incident_at_t")
    private Long atIncidentAtTicks;

    @Column(name = "hospital_queue_entered_at_t")
    private Long hospitalQueueEnteredAtTicks;

    @Column(name = "in_transport_at_t")
    private Long inTransportAtTicks;

    @Column(name = "completed_at_t")
    private Long completedAtTicks;

    @Column(name = "last_outcome_reason", length = 64)
    private String lastOutcomeReason;

    protected EmergencyJpaEntity() {
        // Requerido por JPA
    }

    public EmergencyJpaEntity(
            String id,
            String incidentCategory,
            String nodeId,
            String description,
            String simulatedPriority,
            String requiredEquipment,
            String ruleVersionId,
            String state,
            String assignedAmbulanceId,
            String hospitalId,
            long registeredAtTicks,
            Long queueEnteredAtTicks,
            Long assignedAtTicks,
            Long atIncidentAtTicks,
            Long hospitalQueueEnteredAtTicks,
            Long inTransportAtTicks,
            Long completedAtTicks,
            String lastOutcomeReason
    ) {
        this.id = id;
        this.incidentCategory = incidentCategory;
        this.nodeId = nodeId;
        this.description = description;
        this.simulatedPriority = simulatedPriority;
        this.requiredEquipment = requiredEquipment;
        this.ruleVersionId = ruleVersionId;
        this.state = state;
        this.assignedAmbulanceId = assignedAmbulanceId;
        this.hospitalId = hospitalId;
        this.registeredAtTicks = registeredAtTicks;
        this.queueEnteredAtTicks = queueEnteredAtTicks;
        this.assignedAtTicks = assignedAtTicks;
        this.atIncidentAtTicks = atIncidentAtTicks;
        this.hospitalQueueEnteredAtTicks = hospitalQueueEnteredAtTicks;
        this.inTransportAtTicks = inTransportAtTicks;
        this.completedAtTicks = completedAtTicks;
        this.lastOutcomeReason = lastOutcomeReason;
    }

    public String getId() {
        return id;
    }

    public String getIncidentCategory() {
        return incidentCategory;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getDescription() {
        return description;
    }

    public String getSimulatedPriority() {
        return simulatedPriority;
    }

    public String getRequiredEquipment() {
        return requiredEquipment;
    }

    public String getRuleVersionId() {
        return ruleVersionId;
    }

    public String getState() {
        return state;
    }

    public String getAssignedAmbulanceId() {
        return assignedAmbulanceId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public long getRegisteredAtTicks() {
        return registeredAtTicks;
    }

    public Long getQueueEnteredAtTicks() {
        return queueEnteredAtTicks;
    }

    public Long getAssignedAtTicks() {
        return assignedAtTicks;
    }

    public Long getAtIncidentAtTicks() {
        return atIncidentAtTicks;
    }

    public Long getHospitalQueueEnteredAtTicks() {
        return hospitalQueueEnteredAtTicks;
    }

    public Long getInTransportAtTicks() {
        return inTransportAtTicks;
    }

    public Long getCompletedAtTicks() {
        return completedAtTicks;
    }

    public String getLastOutcomeReason() {
        return lastOutcomeReason;
    }
}
