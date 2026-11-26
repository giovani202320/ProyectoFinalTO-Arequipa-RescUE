package com.arequiparescue.shared.presentation;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Centralizes Spanish presentation labels and semantic CSS badge tones for technical domain codes
 * without altering persisted values, internal enums, or API contracts (DEC-004).
 */
@Component("areUi")
public class AreUiFormatter {

    private static final String DEFAULT_UNKNOWN = "No especificado";

    private static final Map<String, String> EMERGENCY_STATES = Map.of(
            "REGISTERED", "Registrada",
            "WAITING_AMBULANCE", "Esperando ambulancia",
            "ASSIGNED", "Ambulancia asignada",
            "AT_INCIDENT", "En el incidente",
            "WAITING_HOSPITAL", "Esperando hospital",
            "IN_TRANSPORT", "En traslado",
            "COMPLETED", "Atención finalizada"
    );

    private static final Map<String, String> AMBULANCE_STATES = Map.of(
            "AVAILABLE", "Disponible",
            "ASSIGNED", "Asignada a emergencia",
            "AT_INCIDENT", "En el incidente",
            "TRANSPORTING", "En traslado"
    );

    private static final Map<String, String> EQUIPMENT_TYPES = Map.of(
            "BASIC", "Básica",
            "SPECIALIZED", "Especializada"
    );

    private static final Map<String, String> SCENARIO_STATUSES = Map.of(
            "ACTIVE", "Activo",
            "COMPLETED", "Finalizado"
    );

    private static final Map<String, String> EVENT_STATUSES = Map.of(
            "PENDING", "Pendiente",
            "EXECUTED", "Ejecutado",
            "FAILED", "Fallido"
    );

    private static final Map<String, String> EVENT_TYPES = Map.of(
            "AMBULANCE_ARRIVAL_AT_INCIDENT", "Llegada de ambulancia al incidente",
            "WAITING_QUEUE_EVALUATION", "Evaluación de colas de espera",
            "RESOURCE_RELEASE", "Liberación de recurso",
            "EMERGENCY_REGISTRATION", "Registro de emergencia",
            "HOSPITAL_DELIVERY", "Entrega hospitalaria",
            "PATIENT_DISCHARGE", "Alta simulada de paciente"
    );

    private static final Map<String, String> RULE_VERSION_STATUSES = Map.of(
            "ACTIVE", "Activa",
            "INACTIVE", "Inactiva",
            "ARCHIVED", "Archivada"
    );

    private static final Map<String, String> PRIORITY_LEVELS = Map.of(
            "HIGH", "Alta",
            "MEDIUM", "Media",
            "LOW", "Baja"
    );

    private static final Map<String, String> OPERATION_TYPES = Map.of(
            "AUTOMATIC_DISPATCH", "Despacho automático",
            "SUPERVISED_DISPATCH", "Despacho supervisado",
            "WAITING_QUEUE_DISPATCH", "Despacho desde cola de ambulancias",
            "HOSPITAL_RESERVATION", "Reserva hospitalaria",
            "WAITING_HOSPITAL_QUEUE", "Reserva desde cola hospitalaria",
            "HOSPITAL_DELIVERY", "Entrega hospitalaria",
            "HOSPITAL_DISCHARGE", "Alta simulada"
    );

    private static final Map<String, String> IN_PROGRESS_LABELS = Map.of(
            "EN_ESPERA", "En espera de ambulancia",
            "PENDIENTE_LLEGADA_INCIDENTE", "Pendiente de llegada al incidente",
            "EN_PROCESO_TRASLADO", "En proceso de traslado",
            "EN_ESPERA_HOSPITAL", "En espera de hospital",
            "WAITING_HOSPITAL", "Esperando hospital"
    );

    private static final Map<String, String> OPERATION_STATUSES = Map.ofEntries(
            Map.entry("ASSIGNED", "Asignada"),
            Map.entry("WAITING_AMBULANCE", "Esperando ambulancia"),
            Map.entry("MOVED_TO_WAITING_AMBULANCE", "Derivada a cola de ambulancias"),
            Map.entry("CONCURRENCY_RETRY_EXHAUSTED", "Reintentos de concurrencia agotados"),
            Map.entry("NO_ELIGIBLE_AMBULANCE", "Sin ambulancia elegible"),
            Map.entry("IN_TRANSPORT", "En traslado"),
            Map.entry("RESERVED", "Plaza hospitalaria reservada"),
            Map.entry("WAITING_HOSPITAL", "Esperando hospital"),
            Map.entry("MOVED_TO_WAITING_HOSPITAL", "Derivada a cola hospitalaria"),
            Map.entry("NO_ELIGIBLE_HOSPITAL", "Sin hospital elegible"),
            Map.entry("HOSPITAL_RESERVATION_RETRY_EXHAUSTED", "Reintentos de reserva hospitalaria agotados"),
            Map.entry("COMPLETED", "Entrega completada"),
            Map.entry("DISCHARGED", "Alta simulada ejecutada"),
            Map.entry("DUPLICATE_OR_INVALID_STATE", "Operación duplicada o estado inválido"),
            Map.entry("CONFLICT_RELOAD_REQUIRED", "Conflicto concurrente detectado"),
            Map.entry("CONCURRENCY_CONFLICT", "Conflicto de concurrencia"),
            Map.entry("TECHNICAL_ERROR", "Error técnico de persistencia"),
            Map.entry("OK", "Ejecutada correctamente")
    );

    public String emergencyStateLabel(String code) {
        return lookup(EMERGENCY_STATES, code);
    }

    public String ambulanceStateLabel(String code) {
        return lookup(AMBULANCE_STATES, code);
    }

    public String equipmentLabel(String code) {
        return lookup(EQUIPMENT_TYPES, code);
    }

    public String scenarioStatusLabel(String code) {
        return lookup(SCENARIO_STATUSES, code);
    }

    public String eventStatusLabel(String code) {
        return lookup(EVENT_STATUSES, code);
    }

    public String eventTypeLabel(String code) {
        return lookup(EVENT_TYPES, code);
    }

    public String ruleVersionStatusLabel(String code) {
        return lookup(RULE_VERSION_STATUSES, code);
    }

    public String priorityLabel(String code) {
        return lookup(PRIORITY_LEVELS, code);
    }

    public String operationTypeLabel(String code) {
        return lookup(OPERATION_TYPES, code);
    }

    public String inProgressStatusLabel(String code) {
        return lookup(IN_PROGRESS_LABELS, code);
    }

    public String operationStatusLabel(String code) {
        return lookup(OPERATION_STATUSES, code);
    }

    /**
     * Resolves a semantic badge tone class suffix (`success`, `warning`, `danger`, `primary`, `navy`, `neutral`)
     * according to domain context and technical state code.
     */
    public String badgeTone(String context, String code) {
        if (code == null || code.isBlank()) {
            return "neutral";
        }
        String normalizedCode = code.trim().toUpperCase();
        String normalizedContext = context == null ? "" : context.trim().toUpperCase();

        return switch (normalizedContext) {
            case "EMERGENCY" -> switch (normalizedCode) {
                case "COMPLETED" -> "success";
                case "WAITING_AMBULANCE", "WAITING_HOSPITAL" -> "warning";
                case "ASSIGNED", "AT_INCIDENT", "IN_TRANSPORT" -> "primary";
                default -> "neutral";
            };
            case "AMBULANCE" -> switch (normalizedCode) {
                case "AVAILABLE" -> "success";
                case "ASSIGNED", "AT_INCIDENT", "TRANSPORTING" -> "primary";
                default -> "neutral";
            };
            case "EQUIPMENT" -> "SPECIALIZED".equals(normalizedCode) ? "navy" : "neutral";
            case "SCENARIO" -> "ACTIVE".equals(normalizedCode) ? "success" : "navy";
            case "EVENT" -> switch (normalizedCode) {
                case "EXECUTED" -> "success";
                case "PENDING" -> "warning";
                case "FAILED" -> "danger";
                default -> "neutral";
            };
            case "PRIORITY" -> switch (normalizedCode) {
                case "HIGH" -> "danger";
                case "MEDIUM" -> "warning";
                default -> "neutral";
            };
            case "RULE_VERSION" -> "ACTIVE".equals(normalizedCode) ? "success" : "neutral";
            default -> "neutral";
        };
    }

    private String lookup(Map<String, String> dictionary, String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT_UNKNOWN;
        }
        return dictionary.getOrDefault(code.trim().toUpperCase(), code);
    }
}
