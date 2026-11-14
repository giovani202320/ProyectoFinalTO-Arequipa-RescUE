package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code triage_rule_versions} (DEC-003, DEC-016, ADR-0001 Secc. 4.3).
 */
@Entity
@Table(name = "triage_rule_versions")
public class TriageRuleVersionJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "version_number", nullable = false, unique = true)
    private int versionNumber;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "published_at_t", nullable = false)
    private long publishedAtTicks;

    protected TriageRuleVersionJpaEntity() {
        // Requerido por JPA
    }

    public TriageRuleVersionJpaEntity(String id, int versionNumber, String status, long publishedAtTicks) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.status = status;
        this.publishedAtTicks = publishedAtTicks;
    }

    public String getId() {
        return id;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getStatus() {
        return status;
    }

    public long getPublishedAtTicks() {
        return publishedAtTicks;
    }
}
