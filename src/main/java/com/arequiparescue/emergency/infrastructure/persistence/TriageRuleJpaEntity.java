package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code triage_rules} (DEC-003, DEC-016, ADR-0001 Secc. 4.3).
 */
@Entity
@Table(name = "triage_rules")
public class TriageRuleJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "rule_version_id", length = 32, nullable = false)
    private String ruleVersionId;

    @Column(name = "incident_category", length = 32, nullable = false)
    private String incidentCategory;

    @Column(name = "simulated_priority", length = 16, nullable = false)
    private String simulatedPriority;

    @Column(name = "required_equipment", length = 16, nullable = false)
    private String requiredEquipment;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected TriageRuleJpaEntity() {
        // Requerido por JPA
    }

    public TriageRuleJpaEntity(
            String id,
            String ruleVersionId,
            String incidentCategory,
            String simulatedPriority,
            String requiredEquipment,
            boolean active
    ) {
        this.id = id;
        this.ruleVersionId = ruleVersionId;
        this.incidentCategory = incidentCategory;
        this.simulatedPriority = simulatedPriority;
        this.requiredEquipment = requiredEquipment;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getRuleVersionId() {
        return ruleVersionId;
    }

    public String getIncidentCategory() {
        return incidentCategory;
    }

    public String getSimulatedPriority() {
        return simulatedPriority;
    }

    public String getRequiredEquipment() {
        return requiredEquipment;
    }

    public boolean isActive() {
        return active;
    }
}
