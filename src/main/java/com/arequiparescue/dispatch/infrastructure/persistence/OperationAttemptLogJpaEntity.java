package com.arequiparescue.dispatch.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para {@code operation_attempt_logs} (ADR-0002 Decisión 8, RN-CON-008).
 */
@Entity
@Table(name = "operation_attempt_logs")
public class OperationAttemptLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", length = 64, nullable = false)
    private String operationId;

    @Column(name = "operation_type", length = 32, nullable = false)
    private String operationType;

    @Column(name = "emergency_id", length = 32, nullable = false)
    private String emergencyId;

    @Column(name = "candidate_resource_id", length = 32)
    private String candidateResourceId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "outcome_code", length = 48, nullable = false)
    private String outcomeCode;

    @Column(name = "detail_message", length = 500, nullable = false)
    private String detailMessage;

    @Column(name = "logical_t", nullable = false)
    private long logicalTicks;

    protected OperationAttemptLogJpaEntity() {
    }

    public OperationAttemptLogJpaEntity(
            String operationId,
            String operationType,
            String emergencyId,
            String candidateResourceId,
            int attemptNumber,
            String outcomeCode,
            String detailMessage,
            long logicalTicks
    ) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.emergencyId = emergencyId;
        this.candidateResourceId = candidateResourceId;
        this.attemptNumber = attemptNumber;
        this.outcomeCode = outcomeCode;
        this.detailMessage = detailMessage;
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

    public String getCandidateResourceId() {
        return candidateResourceId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getOutcomeCode() {
        return outcomeCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public long getLogicalTicks() {
        return logicalTicks;
    }
}
