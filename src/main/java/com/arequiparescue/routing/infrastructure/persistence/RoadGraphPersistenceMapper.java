package com.arequiparescue.routing.infrastructure.persistence;

import com.arequiparescue.routing.domain.GraphEdge;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Mapeador explícito entre las entidades JPA del grafo y el modelo de dominio puro
 * preservando las invariantes de {@link RoadGraph} (DEC-003, AGENTS.md Secc. 4.3).
 */
@Component
public class RoadGraphPersistenceMapper {

    public GraphNode toDomainNode(GraphNodeJpaEntity entity) {
        Objects.requireNonNull(entity, "La entidad JPA de nodo no puede ser nula.");
        return new GraphNode(
                NodeId.of(entity.getId()),
                entity.getZoneCode(),
                entity.getDisplayName(),
                entity.isIsolatedNode()
        );
    }

    public GraphEdge toDomainEdge(GraphEdgeJpaEntity entity) {
        Objects.requireNonNull(entity, "La entidad JPA de arista no puede ser nula.");
        return new GraphEdge(
                entity.getId(),
                NodeId.of(entity.getSourceNodeId()),
                NodeId.of(entity.getTargetNodeId()),
                entity.getAbstractCost()
        );
    }

    public RoadGraph toDomainGraph(List<GraphNodeJpaEntity> nodeEntities, List<GraphEdgeJpaEntity> edgeEntities) {
        Objects.requireNonNull(nodeEntities, "La lista de entidades JPA de nodos no puede ser nula.");
        Objects.requireNonNull(edgeEntities, "La lista de entidades JPA de aristas no puede ser nula.");
        List<GraphNode> nodes = nodeEntities.stream()
                .map(this::toDomainNode)
                .toList();
        List<GraphEdge> edges = edgeEntities.stream()
                .map(this::toDomainEdge)
                .toList();
        return new RoadGraph(nodes, edges);
    }
}
