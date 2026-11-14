package com.arequiparescue.routing.domain.port;

import com.arequiparescue.routing.domain.RoadGraph;

/**
 * Puerto de dominio para cargar el grafo vial simplificado de Arequipa desde la persistencia
 * sin acoplar el dominio a Spring Data JPA ni Hibernate (DEC-002, DEC-003, ADR-0001).
 */
public interface RoadGraphRepository {

    RoadGraph loadGraph();
}
