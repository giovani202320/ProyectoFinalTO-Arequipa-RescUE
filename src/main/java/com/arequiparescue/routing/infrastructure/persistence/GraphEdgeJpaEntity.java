package com.arequiparescue.routing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA de infraestructura para la tabla {@code graph_edges} (DEC-003, ADR-0001).
 */
@Entity
@Table(name = "graph_edges")
public class GraphEdgeJpaEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "source_node_id", length = 32, nullable = false)
    private String sourceNodeId;

    @Column(name = "target_node_id", length = 32, nullable = false)
    private String targetNodeId;

    @Column(name = "abstract_cost", nullable = false)
    private int abstractCost;

    protected GraphEdgeJpaEntity() {
    }

    public GraphEdgeJpaEntity(String id, String sourceNodeId, String targetNodeId, int abstractCost) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.abstractCost = abstractCost;
    }

    public String getId() {
        return id;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public int getAbstractCost() {
        return abstractCost;
    }
}
