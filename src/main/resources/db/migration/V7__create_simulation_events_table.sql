-- =============================================================================
-- V7__create_simulation_events_table.sql
-- Esquema relacional del Incremento 05 (HU-13):
-- Persistencia de eventos discretos por escenario experimental con ordenamiento
-- determinista y trazabilidad de ejecución.
-- Referencias: DEC-019, DEC-020 (ADR-0001 Secc. 4.3.5), DEC-021 (ADR-0002),
--              RN-RUT-009, CA-SIM-06, CA-SIM-07
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE simulation_events (
    id VARCHAR(64) NOT NULL,
    scenario_id VARCHAR(32) NOT NULL,
    scheduled_at_t BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    precedence_rank INT NOT NULL,
    sequence_number BIGINT NOT NULL,
    subject_id VARCHAR(64) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    executed_at_t BIGINT NULL,
    execution_note VARCHAR(500) NULL,
    CONSTRAINT pk_simulation_events PRIMARY KEY (id),
    CONSTRAINT fk_simulation_events_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios (id),
    CONSTRAINT uq_simulation_events_scenario_seq UNIQUE (scenario_id, sequence_number),
    CONSTRAINT chk_simulation_events_scheduled_t CHECK (scheduled_at_t >= 0),
    CONSTRAINT chk_simulation_events_precedence CHECK (precedence_rank BETWEEN 1 AND 4),
    CONSTRAINT chk_simulation_events_sequence CHECK (sequence_number > 0),
    CONSTRAINT chk_simulation_events_type CHECK (
        event_type IN (
            'RESOURCE_RELEASE',
            'AMBULANCE_ARRIVAL_AT_INCIDENT',
            'WAITING_QUEUE_EVALUATION',
            'EMERGENCY_REGISTRATION'
        )
    ),
    CONSTRAINT chk_simulation_events_status CHECK (status IN ('PENDING', 'EXECUTED', 'FAILED')),
    CONSTRAINT chk_simulation_events_executed_t CHECK (executed_at_t IS NULL OR executed_at_t >= scheduled_at_t)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_simulation_events_deterministic
    ON simulation_events (scenario_id, status, scheduled_at_t, precedence_rank, sequence_number, id);
