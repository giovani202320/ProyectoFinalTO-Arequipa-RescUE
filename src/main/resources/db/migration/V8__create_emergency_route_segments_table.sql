-- =============================================================================
-- V8__create_emergency_route_segments_table.sql
-- Persistencia histórica de segmentos de ruta Dijkstra (ADR-0003):
--   - AMBULANCE_TO_INCIDENT (Tramo A: Ambulancia -> Emergencia)
--   - INCIDENT_TO_HOSPITAL  (Tramo B: Emergencia -> Hospital reservado)
-- Preserva intactas todas las emergencias históricas previas a V8.
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE emergency_route_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    emergency_id VARCHAR(32) NOT NULL,
    segment_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(32) NOT NULL,
    origin_node_id VARCHAR(32) NOT NULL,
    destination_node_id VARCHAR(32) NOT NULL,
    path_nodes VARCHAR(512) NOT NULL,
    abstract_cost INT NOT NULL,
    calculated_at_t BIGINT NOT NULL,
    CONSTRAINT pk_emergency_route_segments PRIMARY KEY (id),
    CONSTRAINT fk_route_segments_emergency FOREIGN KEY (emergency_id) REFERENCES emergencies (id),
    CONSTRAINT fk_route_segments_origin_node FOREIGN KEY (origin_node_id) REFERENCES graph_nodes (id),
    CONSTRAINT fk_route_segments_dest_node FOREIGN KEY (destination_node_id) REFERENCES graph_nodes (id),
    CONSTRAINT uq_emergency_route_segment UNIQUE (emergency_id, segment_type),
    CONSTRAINT chk_route_segments_type CHECK (segment_type IN ('AMBULANCE_TO_INCIDENT', 'INCIDENT_TO_HOSPITAL')),
    CONSTRAINT chk_route_segments_cost CHECK (abstract_cost >= 0),
    CONSTRAINT chk_route_segments_logical_t CHECK (calculated_at_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_emergency_route_segments_emergency ON emergency_route_segments (emergency_id);
