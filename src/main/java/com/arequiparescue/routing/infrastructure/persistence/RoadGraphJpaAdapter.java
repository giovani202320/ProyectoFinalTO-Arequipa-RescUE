package com.arequiparescue.routing.infrastructure.persistence;

import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador de persistencia que implementa {@link RoadGraphRepository} sobre MySQL 8.4 e InnoDB
 * mediante Spring Data JPA y traducción explícita al modelo de dominio (DEC-002, DEC-003, ADR-0001).
 */
@Repository
public class RoadGraphJpaAdapter implements RoadGraphRepository {

    private final SpringDataGraphNodeRepository nodeRepository;
    private final SpringDataGraphEdgeRepository edgeRepository;
    private final RoadGraphPersistenceMapper mapper;

    public RoadGraphJpaAdapter(
            SpringDataGraphNodeRepository nodeRepository,
            SpringDataGraphEdgeRepository edgeRepository,
            RoadGraphPersistenceMapper mapper
    ) {
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
        this.edgeRepository = Objects.requireNonNull(edgeRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    @Transactional(readOnly = true)
    public RoadGraph loadGraph() {
        List<GraphNodeJpaEntity> nodes = nodeRepository.findAllByOrderByIdAsc();
        List<GraphEdgeJpaEntity> edges = edgeRepository.findAllByOrderByIdAsc();
        return mapper.toDomainGraph(nodes, edges);
    }
}
