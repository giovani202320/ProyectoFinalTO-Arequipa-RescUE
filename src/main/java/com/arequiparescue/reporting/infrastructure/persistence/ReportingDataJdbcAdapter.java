package com.arequiparescue.reporting.infrastructure.persistence;

import com.arequiparescue.reporting.domain.ReportingSourceData;
import com.arequiparescue.reporting.domain.port.ReportingDataQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador JDBC de solo lectura para proyectar las tablas operacionales y de trazabilidad hacia
 * {@link ReportingSourceData} sin adquirir bloqueos pesimistas ni acoplarse a la infraestructura de otros módulos.
 */
@Repository
public class ReportingDataJdbcAdapter implements ReportingDataQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public ReportingDataJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportingSourceData loadSourceData(String requestedScenarioId, long fallbackLogicalTicks) {
        List<ReportingSourceData.ScenarioRow> scenarios = jdbcTemplate.query(
                "SELECT id, name, status, current_logical_t FROM simulation_scenarios ORDER BY id ASC",
                (rs, rowNum) -> new ReportingSourceData.ScenarioRow(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getLong("current_logical_t")
                )
        );

        ReportingSourceData.ScenarioRow referenceScenario = resolveReferenceScenario(scenarios, requestedScenarioId);
        String referenceScenarioId = referenceScenario != null ? referenceScenario.id() : null;
        String referenceScenarioStatus = referenceScenario != null ? referenceScenario.status() : "SIN_ESCENARIO";
        long currentLogicalTicks = referenceScenario != null
                ? referenceScenario.currentLogicalTicks()
                : Math.max(0L, fallbackLogicalTicks);

        List<ReportingSourceData.EmergencyRow> emergencies = jdbcTemplate.query(
                """
                SELECT id, state, rule_version_id, assigned_ambulance_id, hospital_id, last_outcome_reason,
                       registered_at_t, queue_entered_at_t, assigned_at_t, at_incident_at_t,
                       hospital_queue_entered_at_t, in_transport_at_t, completed_at_t
                FROM emergencies
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new ReportingSourceData.EmergencyRow(
                        rs.getString("id"),
                        rs.getString("state"),
                        rs.getString("rule_version_id"),
                        rs.getString("assigned_ambulance_id"),
                        rs.getString("hospital_id"),
                        rs.getString("last_outcome_reason"),
                        rs.getLong("registered_at_t"),
                        getNullableLong(rs, "queue_entered_at_t"),
                        getNullableLong(rs, "assigned_at_t"),
                        getNullableLong(rs, "at_incident_at_t"),
                        getNullableLong(rs, "hospital_queue_entered_at_t"),
                        getNullableLong(rs, "in_transport_at_t"),
                        getNullableLong(rs, "completed_at_t")
                )
        );

        List<ReportingSourceData.AmbulanceRow> ambulances = jdbcTemplate.query(
                "SELECT id, equipment_type, state, assigned_emergency_id FROM ambulances ORDER BY id ASC",
                (rs, rowNum) -> new ReportingSourceData.AmbulanceRow(
                        rs.getString("id"),
                        rs.getString("equipment_type"),
                        rs.getString("state"),
                        rs.getString("assigned_emergency_id")
                )
        );

        List<ReportingSourceData.HospitalReservationRow> reservations = jdbcTemplate.query(
                "SELECT emergency_id, hospital_id, status, discharged_at_t FROM hospital_reservations ORDER BY emergency_id ASC",
                (rs, rowNum) -> new ReportingSourceData.HospitalReservationRow(
                        rs.getString("emergency_id"),
                        rs.getString("hospital_id"),
                        rs.getString("status"),
                        getNullableLong(rs, "discharged_at_t")
                )
        );

        List<ReportingSourceData.OperationResultRow> operationResults = jdbcTemplate.query(
                """
                SELECT operation_id, operation_type, emergency_id, assigned_resource_id,
                       total_attempts, final_status, outcome_reason, logical_t
                FROM experiment_operation_results
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new ReportingSourceData.OperationResultRow(
                        rs.getString("operation_id"),
                        rs.getString("operation_type"),
                        rs.getString("emergency_id"),
                        rs.getString("assigned_resource_id"),
                        rs.getInt("total_attempts"),
                        rs.getString("final_status"),
                        rs.getString("outcome_reason"),
                        rs.getLong("logical_t")
                )
        );

        List<ReportingSourceData.AttemptLogRow> attemptLogs = jdbcTemplate.query(
                """
                SELECT operation_id, operation_type, emergency_id, candidate_resource_id,
                       attempt_number, outcome_code, detail_message, logical_t
                FROM operation_attempt_logs
                ORDER BY id ASC
                """,
                (rs, rowNum) -> new ReportingSourceData.AttemptLogRow(
                        rs.getString("operation_id"),
                        rs.getString("operation_type"),
                        rs.getString("emergency_id"),
                        rs.getString("candidate_resource_id"),
                        rs.getInt("attempt_number"),
                        rs.getString("outcome_code"),
                        rs.getString("detail_message"),
                        rs.getLong("logical_t")
                )
        );

        return new ReportingSourceData(
                referenceScenarioId,
                referenceScenarioStatus,
                currentLogicalTicks,
                scenarios,
                emergencies,
                ambulances,
                reservations,
                operationResults,
                attemptLogs
        );
    }

    private static ReportingSourceData.ScenarioRow resolveReferenceScenario(
            List<ReportingSourceData.ScenarioRow> scenarios,
            String requestedScenarioId
    ) {
        if (scenarios.isEmpty()) {
            return null;
        }
        if (requestedScenarioId != null && !requestedScenarioId.isBlank()) {
            String trimmed = requestedScenarioId.trim();
            for (ReportingSourceData.ScenarioRow row : scenarios) {
                if (trimmed.equals(row.id())) {
                    return row;
                }
            }
        }
        for (ReportingSourceData.ScenarioRow row : scenarios) {
            if ("ACTIVE".equals(row.status())) {
                return row;
            }
        }
        return scenarios.getLast();
    }

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
