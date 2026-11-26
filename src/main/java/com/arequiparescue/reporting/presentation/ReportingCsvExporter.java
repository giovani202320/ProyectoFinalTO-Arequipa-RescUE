package com.arequiparescue.reporting.presentation;

import com.arequiparescue.reporting.domain.ExperimentalEmergencyRecord;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exportador CSV (RFC 4180, UTF-8 con BOM para Excel) del registro funcional experimental trazable
 * con protección explícita contra inyección de fórmulas CSV (P1-2).
 */
public final class ReportingCsvExporter {

    private static final String UTF8_BOM = "\uFEFF";
    private static final String CRLF = "\r\n";

    static final List<String> COLUMNS = List.of(
            "emergency_id",
            "state",
            "priority",
            "category",
            "rule_version_id",
            "assigned_ambulance_id",
            "hospital_id",
            "last_outcome_reason",
            "registered_at_t",
            "queue_entered_at_t",
            "assigned_at_t",
            "at_incident_at_t",
            "hospital_queue_entered_at_t",
            "in_transport_at_t",
            "completed_at_t",
            "discharged_at_t",
            "final_operations_count",
            "attempt_logs_count"
    );

    private ReportingCsvExporter() {
    }

    public static byte[] exportTraceabilityCsv(List<ExperimentalEmergencyRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);
        sb.append(String.join(",", COLUMNS)).append(CRLF);

        if (records != null) {
            for (ExperimentalEmergencyRecord r : records) {
                sb.append(formatCell(r.emergencyId())).append(',')
                        .append(formatCell(r.currentState())).append(',')
                        .append(formatCell(r.simulatedPriority())).append(',')
                        .append(formatCell(r.incidentCategory())).append(',')
                        .append(formatCell(r.ruleVersionId())).append(',')
                        .append(formatCell(r.assignedAmbulanceId())).append(',')
                        .append(formatCell(r.hospitalId())).append(',')
                        .append(formatCell(r.lastOutcomeReason())).append(',')
                        .append(r.registeredAtTicks()).append(',')
                        .append(formatNullableLong(r.queueEnteredAtTicks())).append(',')
                        .append(formatNullableLong(r.assignedAtTicks())).append(',')
                        .append(formatNullableLong(r.atIncidentAtTicks())).append(',')
                        .append(formatNullableLong(r.hospitalQueueEnteredAtTicks())).append(',')
                        .append(formatNullableLong(r.inTransportAtTicks())).append(',')
                        .append(formatNullableLong(r.completedAtTicks())).append(',')
                        .append(formatNullableLong(r.dischargedAtTicks())).append(',')
                        .append(r.operationResults() != null ? r.operationResults().size() : 0).append(',')
                        .append(r.attemptLogs() != null ? r.attemptLogs().size() : 0)
                        .append(CRLF);
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String formatCell(String raw) {
        if (raw == null) {
            return "";
        }
        String sanitized = neutralizeFormulaInjection(raw);
        boolean needsQuotes = sanitized.indexOf(',') >= 0
                || sanitized.indexOf('"') >= 0
                || sanitized.indexOf('\n') >= 0
                || sanitized.indexOf('\r') >= 0
                || !sanitized.equals(raw);
        if (!needsQuotes) {
            return sanitized;
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }

    static String neutralizeFormulaInjection(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (isDangerousLeadingChar(first)) {
            return "'" + value;
        }
        // También neutraliza espacios o caracteres de control iniciales seguidos de un disparador de fórmula
        int idx = 0;
        while (idx < value.length() && (value.charAt(idx) <= ' ')) {
            idx++;
        }
        if (idx > 0 && idx < value.length() && isDangerousLeadingChar(value.charAt(idx))) {
            return "'" + value;
        }
        return value;
    }

    private static boolean isDangerousLeadingChar(char ch) {
        return ch == '=' || ch == '+' || ch == '-' || ch == '@' || ch == '\t' || ch == '\r' || ch == '\n';
    }

    private static String formatNullableLong(Long value) {
        return value != null ? Long.toString(value) : "";
    }
}
