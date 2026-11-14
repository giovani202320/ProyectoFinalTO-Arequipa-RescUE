package com.arequiparescue.routing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA de infraestructura para la tabla {@code graph_nodes} (DEC-003, ADR-0001).
 */
@Entity
@Table(name = "graph_nodes")
public class GraphNodeJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "zone_code", length = 64, nullable = false, unique = true)
    private String zoneCode;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Column(name = "isolated_node", nullable = false)
    private boolean isolatedNode;

    protected GraphNodeJpaEntity() {
    }

    public GraphNodeJpaEntity(String id, String zoneCode, String displayName, boolean isolatedNode) {
        this.id = id;
        this.zoneCode = zoneCode;
        this.displayName = displayName;
        this.isolatedNode = isolatedNode;
    }

    public String getId() {
        return id;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isIsolatedNode() {
        return isolatedNode;
    }
}
