package com.arequiparescue.dispatch.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para {@code experiment_operation_results} (ADR-0002 Decisión 8, RN-CON-008).
 */
@Entity
@Table(name = "experiment_operation_results")
public class ExperimentOperationResultJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", length = 64, nullable = false, unique = true)
    private String operationId;

    @Column(name = "operation_type", length = 32, nullable = false)
    private String operationType;

    @Column(name = "emergency_id", length = 32, nullable = false)
    private String emergencyId;

    @Column(name = "assigned_resource_id", length = 32)
    private String assignedResourceId;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    @Column(name = "final_status", length = 48, nullable = false)
    private String finalStatus;

    @Column(name = "outcome_reason", length = 500, nullable = false)
    private String outcomeReason;

    @Column(name = "logical_t", nullable = false)
    private long logicalTicks;

    protected ExperimentOperationResultJpaEntity() {
    }

    public ExperimentOperationResultJpaEntity(
            String operationId,
            String operationType,
            String emergencyId,
            String assignedResourceId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    ) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.emergencyId = emergencyId;
        this.assignedResourceId = assignedResourceId;
        this.totalAttempts = totalAttempts;
        this.finalStatus = finalStatus;
        this.outcomeReason = outcomeReason;
        this.logicalTicks = logicalTicks;
    }

    public Long getId() {
        return id;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getEmergencyId() {
        return emergencyId;
    }

    public String getAssignedResourceId() {
        return assignedResourceId;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }

    public long getLogicalTicks() {
        return logicalTicks;
    }
}
