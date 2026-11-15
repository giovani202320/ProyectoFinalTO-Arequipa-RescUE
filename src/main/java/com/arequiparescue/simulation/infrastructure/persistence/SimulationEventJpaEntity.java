package com.arequiparescue.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code simulation_events} (Incremento 05 — HU-13).
 */
@Entity
@Table(name = "simulation_events")
public class SimulationEventJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "scenario_id", length = 32, nullable = false)
    private String scenarioId;

    @Column(name = "scheduled_at_t", nullable = false)
    private long scheduledAtTicks;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "precedence_rank", nullable = false)
    private int precedenceRank;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "subject_id", length = 64, nullable = false)
    private String subjectId;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "executed_at_t")
    private Long executedAtTicks;

    @Column(name = "execution_note", length = 500)
    private String executionNote;

    protected SimulationEventJpaEntity() {
        // Requerido por JPA
    }

    public SimulationEventJpaEntity(
            String id,
            String scenarioId,
            long scheduledAtTicks,
            String eventType,
            int precedenceRank,
            long sequenceNumber,
            String subjectId,
            String description,
            String status,
            Long executedAtTicks,
            String executionNote
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.scheduledAtTicks = scheduledAtTicks;
        this.eventType = eventType;
        this.precedenceRank = precedenceRank;
        this.sequenceNumber = sequenceNumber;
        this.subjectId = subjectId;
        this.description = description;
        this.status = status;
        this.executedAtTicks = executedAtTicks;
        this.executionNote = executionNote;
    }

    public String getId() {
        return id;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public long getScheduledAtTicks() {
        return scheduledAtTicks;
    }

    public String getEventType() {
        return eventType;
    }

    public int getPrecedenceRank() {
        return precedenceRank;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getExecutedAtTicks() {
        return executedAtTicks;
    }

    public void setExecutedAtTicks(Long executedAtTicks) {
        this.executedAtTicks = executedAtTicks;
    }

    public String getExecutionNote() {
        return executionNote;
    }

    public void setExecutionNote(String executionNote) {
        this.executionNote = executionNote;
    }
}
