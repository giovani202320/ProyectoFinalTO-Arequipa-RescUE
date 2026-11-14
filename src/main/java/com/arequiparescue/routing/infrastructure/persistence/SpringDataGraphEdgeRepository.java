package com.arequiparescue.routing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link GraphEdgeJpaEntity}.
 */
public interface SpringDataGraphEdgeRepository extends JpaRepository<GraphEdgeJpaEntity, String> {

    List<GraphEdgeJpaEntity> findAllByOrderByIdAsc();
}
