package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA de infraestructura para la tabla {@code emergency_route_segments} (ADR-0003).
 */
@Entity
@Table(name = "emergency_route_segments")
public class EmergencyRouteSegmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emergency_id", nullable = false, length = 32)
    private String emergencyId;

    @Column(name = "segment_type", nullable = false, length = 32)
    private String segmentType;

    @Column(name = "resource_id", nullable = false, length = 32)
    private String resourceId;

    @Column(name = "origin_node_id", nullable = false, length = 32)
    private String originNodeId;

    @Column(name = "destination_node_id", nullable = false, length = 32)
    private String destinationNodeId;

    @Column(name = "path_nodes", nullable = false, length = 512)
    private String pathNodes;

    @Column(name = "abstract_cost", nullable = false)
    private int abstractCost;

    @Column(name = "calculated_at_t", nullable = false)
    private long calculatedAtT;

    protected EmergencyRouteSegmentJpaEntity() {
    }

    public EmergencyRouteSegmentJpaEntity(
            String emergencyId,
            String segmentType,
            String resourceId,
            String originNodeId,
            String destinationNodeId,
            String pathNodes,
            int abstractCost,
            long calculatedAtT
    ) {
        this.emergencyId = emergencyId;
        this.segmentType = segmentType;
        this.resourceId = resourceId;
        this.originNodeId = originNodeId;
        this.destinationNodeId = destinationNodeId;
        this.pathNodes = pathNodes;
        this.abstractCost = abstractCost;
        this.calculatedAtT = calculatedAtT;
    }

    public Long getId() {
        return id;
    }

    public String getEmergencyId() {
        return emergencyId;
    }

    public String getSegmentType() {
        return segmentType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOriginNodeId() {
        return originNodeId;
    }

    public String getDestinationNodeId() {
        return destinationNodeId;
    }

    public String getPathNodes() {
        return pathNodes;
    }

    public int getAbstractCost() {
        return abstractCost;
    }

    public long getCalculatedAtT() {
        return calculatedAtT;
    }
}
