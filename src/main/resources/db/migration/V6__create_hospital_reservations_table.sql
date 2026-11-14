-- =============================================================================
-- V6__create_hospital_reservations_table.sql
-- Esquema relacional del Incremento 04 (primera entrega: HU-09 y HU-10):
-- Registro único de reserva hospitalaria activa por emergencia (evita doble reserva)
-- e índice FIFO para la cola WAITING_HOSPITAL.
-- Referencias: DEC-018, DEC-020 (ADR-0001 Secc. 4.3.5), DEC-021 (ADR-0002),
--              RN-HOS-004, RN-HOS-007, RN-HOS-010
-- Motor objetivo: MySQL 8.4 LTS (InnoDB)
-- =============================================================================

CREATE TABLE hospital_reservations (
    id VARCHAR(64) NOT NULL,
    emergency_id VARCHAR(32) NOT NULL,
    hospital_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reserved_at_t BIGINT NOT NULL,
    occupied_at_t BIGINT NULL,
    discharged_at_t BIGINT NULL,
    CONSTRAINT pk_hospital_reservations PRIMARY KEY (id),
    CONSTRAINT uq_hospital_reservations_emergency UNIQUE (emergency_id),
    CONSTRAINT fk_hospital_reservations_emergency FOREIGN KEY (emergency_id) REFERENCES emergencies (id),
    CONSTRAINT fk_hospital_reservations_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    CONSTRAINT chk_hospital_reservations_status CHECK (status IN ('RESERVED', 'OCCUPIED', 'DISCHARGED')),
    CONSTRAINT chk_hospital_reservations_reserved_t CHECK (reserved_at_t >= 0),
    CONSTRAINT chk_hospital_reservations_occupied_t CHECK (occupied_at_t IS NULL OR occupied_at_t >= reserved_at_t),
    CONSTRAINT chk_hospital_reservations_discharged_t CHECK (discharged_at_t IS NULL OR discharged_at_t >= occupied_at_t)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_hospital_reservations_hospital_status ON hospital_reservations (hospital_id, status);
