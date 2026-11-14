-- =============================================================================
-- V1__init_schema.sql
-- Esquema relacional mínimo del Incremento 01: Grafo vial simplificado de Arequipa
-- Referencias: DEC-013, DEC-019 (RN-RUT-006), DEC-020 (ADR-0001 Secc. 4.3)
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE graph_nodes (
    id VARCHAR(32) NOT NULL,
    zone_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    isolated_node BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_graph_nodes PRIMARY KEY (id),
    CONSTRAINT uq_graph_nodes_zone_code UNIQUE (zone_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE graph_edges (
    id VARCHAR(32) NOT NULL,
    source_node_id VARCHAR(32) NOT NULL,
    target_node_id VARCHAR(32) NOT NULL,
    abstract_cost INT NOT NULL,
    CONSTRAINT pk_graph_edges PRIMARY KEY (id),
    CONSTRAINT fk_graph_edges_source FOREIGN KEY (source_node_id) REFERENCES graph_nodes (id),
    CONSTRAINT fk_graph_edges_target FOREIGN KEY (target_node_id) REFERENCES graph_nodes (id),
    CONSTRAINT uq_graph_edges_pair UNIQUE (source_node_id, target_node_id),
    CONSTRAINT chk_graph_edges_positive_cost CHECK (abstract_cost > 0),
    CONSTRAINT chk_graph_edges_distinct_nodes CHECK (source_node_id <> target_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_graph_edges_source ON graph_edges (source_node_id);
CREATE INDEX idx_graph_edges_target ON graph_edges (target_node_id);
