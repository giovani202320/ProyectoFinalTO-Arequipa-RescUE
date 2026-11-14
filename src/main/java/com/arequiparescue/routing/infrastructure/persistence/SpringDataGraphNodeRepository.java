package com.arequiparescue.routing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link GraphNodeJpaEntity}.
 */
public interface SpringDataGraphNodeRepository extends JpaRepository<GraphNodeJpaEntity, String> {

    List<GraphNodeJpaEntity> findAllByOrderByIdAsc();
}
