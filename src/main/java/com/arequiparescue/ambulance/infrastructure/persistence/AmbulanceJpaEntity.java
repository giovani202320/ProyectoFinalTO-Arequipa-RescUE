package com.arequiparescue.ambulance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code ambulances} (DEC-003, DEC-019, ADR-0001 Secc. 4.3, ADR-0002 Secc. 4.1).
 */
@Entity
@Table(name = "ambulances")
public class AmbulanceJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "equipment_type", length = 16, nullable = false)
    private String equipmentType;

    @Column(name = "current_node_id", length = 32, nullable = false)
    private String currentNodeId;

    @Column(name = "state", length = 32, nullable = false)
    private String state;

    @Column(name = "assigned_emergency_id", length = 32, unique = true)
    private String assignedEmergencyId;

    protected AmbulanceJpaEntity() {
        // Requerido por JPA
    }

    public AmbulanceJpaEntity(
            String id,
            String equipmentType,
            String currentNodeId,
            String state,
            String assignedEmergencyId
    ) {
        this.id = id;
        this.equipmentType = equipmentType;
        this.currentNodeId = currentNodeId;
        this.state = state;
        this.assignedEmergencyId = assignedEmergencyId;
    }

    public String getId() {
        return id;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public String getState() {
        return state;
    }

    public String getAssignedEmergencyId() {
        return assignedEmergencyId;
    }
}
