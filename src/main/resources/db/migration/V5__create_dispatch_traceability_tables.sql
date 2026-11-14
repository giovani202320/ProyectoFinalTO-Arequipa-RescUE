-- =============================================================================
-- V5__create_dispatch_traceability_tables.sql
-- Esquema relacional del Incremento 03 (primera entrega: HU-05 y HU-06):
-- Trazabilidad atómica e independiente de intentos y resultados de operaciones
-- de despacho/asignación conforme a ADR-0002 (Decisión 8) y RN-CON-008.
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE operation_attempt_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    emergency_id VARCHAR(32) NOT NULL,
    candidate_resource_id VARCHAR(32) NULL,
    attempt_number INT NOT NULL,
    outcome_code VARCHAR(48) NOT NULL,
    detail_message VARCHAR(500) NOT NULL,
    logical_t BIGINT NOT NULL,
    CONSTRAINT pk_operation_attempt_logs PRIMARY KEY (id),
    CONSTRAINT uq_operation_attempt_number UNIQUE (operation_id, attempt_number),
    CONSTRAINT fk_attempt_logs_emergency FOREIGN KEY (emergency_id) REFERENCES emergencies (id),
    CONSTRAINT chk_attempt_logs_number CHECK (attempt_number >= 1),
    CONSTRAINT chk_attempt_logs_logical_t CHECK (logical_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_attempt_logs_emergency ON operation_attempt_logs (emergency_id, attempt_number);

CREATE TABLE experiment_operation_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    emergency_id VARCHAR(32) NOT NULL,
    assigned_resource_id VARCHAR(32) NULL,
    total_attempts INT NOT NULL,
    final_status VARCHAR(48) NOT NULL,
    outcome_reason VARCHAR(500) NOT NULL,
    logical_t BIGINT NOT NULL,
    CONSTRAINT pk_experiment_operation_results PRIMARY KEY (id),
    CONSTRAINT uq_experiment_operation_id UNIQUE (operation_id),
    CONSTRAINT fk_op_results_emergency FOREIGN KEY (emergency_id) REFERENCES emergencies (id),
    CONSTRAINT chk_op_results_attempts CHECK (total_attempts >= 0),
    CONSTRAINT chk_op_results_logical_t CHECK (logical_t >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_op_results_emergency ON experiment_operation_results (emergency_id);
