-- =============================================================================
-- V3__create_emergencies_and_triage_rules.sql
-- Esquema relacional y semilla inicial del Incremento 02 (primera entrega):
-- Versiones inmutables de reglas de clasificación (RUL-V01), secuencia transaccional
-- de identificadores canónicos (EMG-XXXXX) y registro de emergencias (HU-01, HU-02)
-- Referencias: DEC-010, DEC-016, DEC-019, DEC-020 (ADR-0001 Secc. 4.3), DEC-021 (ADR-0002), RN-EME-001 a RN-EME-011, RN-RUT-007
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE emergency_sequences (
    sequence_name VARCHAR(32) NOT NULL,
    next_val BIGINT NOT NULL,
    CONSTRAINT pk_emergency_sequences PRIMARY KEY (sequence_name),
    CONSTRAINT chk_emergency_sequences_positive CHECK (next_val >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE triage_rule_versions (
    id VARCHAR(32) NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    published_at_t BIGINT NOT NULL,
    CONSTRAINT pk_triage_rule_versions PRIMARY KEY (id),
    CONSTRAINT uq_triage_rule_versions_number UNIQUE (version_number),
    CONSTRAINT chk_triage_rule_versions_number CHECK (version_number > 0),
    CONSTRAINT chk_triage_rule_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ACTIVE')),
    CONSTRAINT chk_triage_rule_versions_published_at CHECK (published_at_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE triage_rules (
    id VARCHAR(32) NOT NULL,
    rule_version_id VARCHAR(32) NOT NULL,
    incident_category VARCHAR(32) NOT NULL,
    simulated_priority VARCHAR(16) NOT NULL,
    required_equipment VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_triage_rules PRIMARY KEY (id),
    CONSTRAINT fk_triage_rules_version FOREIGN KEY (rule_version_id) REFERENCES triage_rule_versions (id),
    CONSTRAINT uq_triage_rules_version_category UNIQUE (rule_version_id, incident_category),
    CONSTRAINT chk_triage_rules_category CHECK (incident_category IN ('INC-A', 'INC-B', 'INC-C', 'INC-D')),
    CONSTRAINT chk_triage_rules_priority CHECK (simulated_priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_triage_rules_equipment CHECK (required_equipment IN ('BASIC', 'SPECIALIZED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_triage_rules_version ON triage_rules (rule_version_id);

CREATE TABLE emergencies (
    id VARCHAR(32) NOT NULL,
    incident_category VARCHAR(32) NOT NULL,
    node_id VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    simulated_priority VARCHAR(16) NOT NULL,
    required_equipment VARCHAR(16) NOT NULL,
    rule_version_id VARCHAR(32) NOT NULL,
    state VARCHAR(32) NOT NULL,
    assigned_ambulance_id VARCHAR(32) NULL,
    hospital_id VARCHAR(32) NULL,
    registered_at_t BIGINT NOT NULL,
    queue_entered_at_t BIGINT NULL,
    assigned_at_t BIGINT NULL,
    at_incident_at_t BIGINT NULL,
    hospital_queue_entered_at_t BIGINT NULL,
    in_transport_at_t BIGINT NULL,
    completed_at_t BIGINT NULL,
    last_outcome_reason VARCHAR(64) NULL,
    CONSTRAINT pk_emergencies PRIMARY KEY (id),
    CONSTRAINT fk_emergencies_node FOREIGN KEY (node_id) REFERENCES graph_nodes (id),
    CONSTRAINT fk_emergencies_rule_version FOREIGN KEY (rule_version_id) REFERENCES triage_rule_versions (id),
    CONSTRAINT chk_emergencies_category CHECK (incident_category IN ('INC-A', 'INC-B', 'INC-C', 'INC-D')),
    CONSTRAINT chk_emergencies_priority CHECK (simulated_priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_emergencies_equipment CHECK (required_equipment IN ('BASIC', 'SPECIALIZED')),
    CONSTRAINT chk_emergencies_state CHECK (state IN (
        'REGISTERED',
        'WAITING_AMBULANCE',
        'ASSIGNED',
        'AT_INCIDENT',
        'WAITING_HOSPITAL',
        'IN_TRANSPORT',
        'COMPLETED'
    )),
    CONSTRAINT chk_emergencies_registered_at CHECK (registered_at_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_emergencies_state ON emergencies (state);
CREATE INDEX idx_emergencies_registered_order ON emergencies (registered_at_t, id);

-- =============================================================================
-- Semilla inicial de secuencia transaccional y versión activa RUL-V01 (DEC-016)
-- =============================================================================

INSERT INTO emergency_sequences (sequence_name, next_val) VALUES
    ('EMERGENCY', 1);

INSERT INTO triage_rule_versions (id, version_number, status, published_at_t) VALUES
    ('RUL-V01', 1, 'ACTIVE', 0);

INSERT INTO triage_rules (id, rule_version_id, incident_category, simulated_priority, required_equipment, active) VALUES
    ('RUL-V01-INC-A', 'RUL-V01', 'INC-A', 'HIGH',   'SPECIALIZED', TRUE),
    ('RUL-V01-INC-B', 'RUL-V01', 'INC-B', 'MEDIUM', 'BASIC',       TRUE),
    ('RUL-V01-INC-C', 'RUL-V01', 'INC-C', 'LOW',    'BASIC',       TRUE),
    ('RUL-V01-INC-D', 'RUL-V01', 'INC-D', 'HIGH',   'BASIC',       TRUE);
