package com.arequiparescue.hospital.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code hospitals} (DEC-003, DEC-018, ADR-0001 Secc. 4.3, ADR-0002 Secc. 4.2).
 * Representa exclusivamente capacidad numérica simulada C, R, O sin especialidades clínicas.
 */
@Entity
@Table(name = "hospitals")
public class HospitalJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "node_id", length = 32, nullable = false)
    private String nodeId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "capacity_total", nullable = false)
    private int capacityTotal;

    @Column(name = "reserved_slots", nullable = false)
    private int reservedSlots;

    @Column(name = "occupied_slots", nullable = false)
    private int occupiedSlots;

    protected HospitalJpaEntity() {
        // Requerido por JPA
    }

    public HospitalJpaEntity(
            String id,
            String name,
            String nodeId,
            boolean enabled,
            int capacityTotal,
            int reservedSlots,
            int occupiedSlots
    ) {
        this.id = id;
        this.name = name;
        this.nodeId = nodeId;
        this.enabled = enabled;
        this.capacityTotal = capacityTotal;
        this.reservedSlots = reservedSlots;
        this.occupiedSlots = occupiedSlots;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCapacityTotal() {
        return capacityTotal;
    }

    public int getReservedSlots() {
        return reservedSlots;
    }

    public int getOccupiedSlots() {
        return occupiedSlots;
    }
}
