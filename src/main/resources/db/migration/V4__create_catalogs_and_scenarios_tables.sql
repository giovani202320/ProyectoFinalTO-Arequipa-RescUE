-- =============================================================================
-- V4__create_catalogs_and_scenarios_tables.sql
-- Esquema relacional del Incremento 02 (segunda entrega: HU-03 y HU-04):
-- Catálogo de ambulancias, catálogo de hospitales (capacidad C/R/O/D sin especialidades),
-- control de estado de escenarios experimentales activos y secuencias transaccionales.
-- Referencias: DEC-016, DEC-018, DEC-019, DEC-020 (ADR-0001 Secc. 4.3), DEC-021 (ADR-0002),
--              RN-EME-011, RN-EME-012, RN-RUT-006, RN-RUT-007, RN-HOS-001, RN-HOS-004, RN-HOS-015
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE simulation_scenarios (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL,
    current_logical_t BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_simulation_scenarios PRIMARY KEY (id),
    CONSTRAINT chk_simulation_scenarios_status CHECK (status IN ('ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_simulation_scenarios_logical_t CHECK (current_logical_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_simulation_scenarios_status ON simulation_scenarios (status);

CREATE TABLE ambulances (
    id VARCHAR(32) NOT NULL,
    equipment_type VARCHAR(16) NOT NULL,
    current_node_id VARCHAR(32) NOT NULL,
    state VARCHAR(32) NOT NULL,
    assigned_emergency_id VARCHAR(32) NULL,
    CONSTRAINT pk_ambulances PRIMARY KEY (id),
    CONSTRAINT fk_ambulances_node FOREIGN KEY (current_node_id) REFERENCES graph_nodes (id),
    CONSTRAINT fk_ambulances_emergency FOREIGN KEY (assigned_emergency_id) REFERENCES emergencies (id),
    CONSTRAINT uq_ambulances_assigned_emergency UNIQUE (assigned_emergency_id),
    CONSTRAINT chk_ambulances_equipment CHECK (equipment_type IN ('BASIC', 'SPECIALIZED')),
    CONSTRAINT chk_ambulances_state CHECK (state IN (
        'AVAILABLE',
        'ASSIGNED',
        'AT_INCIDENT',
        'TRANSPORTING'
    )),
    CONSTRAINT chk_ambulances_active_binding CHECK (
        (state = 'AVAILABLE' AND assigned_emergency_id IS NULL)
        OR (state IN ('ASSIGNED', 'AT_INCIDENT', 'TRANSPORTING') AND assigned_emergency_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_ambulances_state_equipment ON ambulances (state, equipment_type);
CREATE INDEX idx_ambulances_node ON ambulances (current_node_id);

CREATE TABLE hospitals (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    node_id VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    capacity_total INT NOT NULL,
    reserved_slots INT NOT NULL DEFAULT 0,
    occupied_slots INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_hospitals PRIMARY KEY (id),
    CONSTRAINT fk_hospitals_node FOREIGN KEY (node_id) REFERENCES graph_nodes (id),
    CONSTRAINT chk_hospitals_capacity_invariants CHECK (
        capacity_total >= 0
        AND reserved_slots >= 0
        AND occupied_slots >= 0
        AND reserved_slots + occupied_slots <= capacity_total
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_hospitals_enabled_node ON hospitals (enabled, node_id);

-- =============================================================================
-- Secuencias transaccionales para AMB-XX, HOSP-XX y RUL-VXX (ADR-0002)
-- =============================================================================

INSERT INTO emergency_sequences (sequence_name, next_val) VALUES
    ('AMBULANCE', 1),
    ('HOSPITAL', 1),
    ('TRIAGE_RULE_VERSION', 2);
