-- =============================================================================
-- demo-seed.sql — Dataset de demostración reproducible para ARE
-- Base objetivo obligatoria: arequipa_rescue_demo
-- Requisitos:
--   1. Flyway V1–V7 aplicado previamente sobre arequipa_rescue_demo.
--   2. Ejecutar con un usuario con privilegios de procedimiento en esa base
--      (por ejemplo root en Docker o usuario con CREATE/ALTER ROUTINE).
-- Seguridad:
--   - Verifica DATABASE() = 'arequipa_rescue_demo' antes de consultar tablas.
--   - Verifica existencia de flyway_schema_history y versión 7 exitosa.
--   - Aborta de forma segura si ya existen datos en ambulancias, hospitales o emergencias.
--   - Usa START TRANSACTION + EXIT HANDLER con ROLLBACK + RESIGNAL.
--   - No desactiva FOREIGN_KEY_CHECKS ni usa TRUNCATE/DROP/DELETE de datos.
-- =============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_are_seed_demo //

CREATE PROCEDURE sp_are_seed_demo()
BEGIN
    DECLARE v_flyway_table INT DEFAULT 0;
    DECLARE v_flyway_v7 INT DEFAULT 0;
    DECLARE v_existing_rows INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL OR DATABASE() <> 'arequipa_rescue_demo' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORTADO: demo-seed.sql solo puede ejecutarse sobre la base arequipa_rescue_demo.';
    END IF;

    SELECT COUNT(*) INTO v_flyway_table
    FROM information_schema.tables
    WHERE table_schema = 'arequipa_rescue_demo'
      AND table_name = 'flyway_schema_history';

    IF v_flyway_table = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORTADO: No existe flyway_schema_history en arequipa_rescue_demo. Ejecute la aplicación primero.';
    END IF;

    SELECT COUNT(*) INTO v_flyway_v7
    FROM flyway_schema_history
    WHERE version = '7' AND success = 1;

    IF v_flyway_v7 = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORTADO: Flyway V1-V7 no ha finalizado correctamente en arequipa_rescue_demo.';
    END IF;

    SELECT (
        (SELECT COUNT(*) FROM ambulances)
        + (SELECT COUNT(*) FROM hospitals)
        + (SELECT COUNT(*) FROM emergencies)
    ) INTO v_existing_rows;

    IF v_existing_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'OMITIDO: arequipa_rescue_demo ya contiene datos. No se sobreescriben registros.';
    END IF;

    START TRANSACTION;

    INSERT INTO ambulances (id, equipment_type, current_node_id, state, assigned_emergency_id) VALUES
        ('AMB-01', 'SPECIALIZED', 'N01', 'AVAILABLE', NULL),
        ('AMB-02', 'BASIC',       'N02', 'AVAILABLE', NULL),
        ('AMB-03', 'BASIC',       'N04', 'AVAILABLE', NULL),
        ('AMB-04', 'SPECIALIZED', 'N06', 'AVAILABLE', NULL);

    INSERT INTO hospitals (id, name, node_id, enabled, capacity_total, reserved_slots, occupied_slots) VALUES
        ('HOSP-01', 'Hospital General Demo',     'N05', TRUE, 5, 0, 0),
        ('HOSP-02', 'Hospital Norte Demo',       'N07', TRUE, 3, 0, 0),
        ('HOSP-03', 'Posta Capacidad Mínima',    'N03', TRUE, 1, 0, 0);

    INSERT INTO emergencies (
        id, incident_category, node_id, description,
        simulated_priority, required_equipment, rule_version_id, state,
        assigned_ambulance_id, hospital_id, registered_at_t
    ) VALUES
        ('EMG-00001', 'INC-A', 'N01', 'Caso demo crítico para ambulancia especializada',
         'HIGH', 'SPECIALIZED', 'RUL-V01', 'REGISTERED', NULL, NULL, 0),
        ('EMG-00002', 'INC-B', 'N03', 'Caso demo intermedio para ruta multicamino',
         'MEDIUM', 'BASIC', 'RUL-V01', 'REGISTERED', NULL, NULL, 0),
        ('EMG-00003', 'INC-C', 'N10', 'Caso demo opcional en nodo aislado N10',
         'LOW', 'BASIC', 'RUL-V01', 'REGISTERED', NULL, NULL, 0);

    UPDATE emergency_sequences SET next_val = 5 WHERE sequence_name = 'AMBULANCE';
    UPDATE emergency_sequences SET next_val = 4 WHERE sequence_name = 'HOSPITAL';
    UPDATE emergency_sequences SET next_val = 4 WHERE sequence_name = 'EMERGENCY';

    COMMIT;
END //

DELIMITER ;

CALL sp_are_seed_demo();
DROP PROCEDURE IF EXISTS sp_are_seed_demo;
