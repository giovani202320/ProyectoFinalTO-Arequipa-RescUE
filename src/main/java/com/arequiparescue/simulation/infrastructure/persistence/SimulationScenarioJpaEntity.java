package com.arequiparescue.simulation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code simulation_scenarios} (ADR-0001 Secc. 4.3.5, ADR-0002 Secc. 4.7).
 */
@Entity
@Table(name = "simulation_scenarios")
public class SimulationScenarioJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "current_logical_t", nullable = false)
    private long currentLogicalTicks;

    protected SimulationScenarioJpaEntity() {
        // Requerido por JPA
    }

    public SimulationScenarioJpaEntity(String id, String name, String status, long currentLogicalTicks) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.currentLogicalTicks = currentLogicalTicks;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCurrentLogicalTicks() {
        return currentLogicalTicks;
    }
}
