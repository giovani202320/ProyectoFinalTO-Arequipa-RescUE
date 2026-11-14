package com.arequiparescue.emergency.application;

import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleEntry;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.emergency.domain.port.TriageRuleVersionRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de aplicación para la publicación y activación de versiones inmutables del catálogo
 * de reglas de clasificación (ACT-03 Administrador) y la consulta en modo de solo lectura
 * para Operador de recepción (ACT-01) y Despachador (ACT-02) conforme a HU-04, RN-EME-011 y RN-EME-012.
 */
@Service
public class TriageRuleCatalogApplicationService {

    private final TriageRuleVersionRepository triageRuleVersionRepository;
    private final ActiveScenarioChecker activeScenarioChecker;
    private final LogicalClock logicalClock;

    public TriageRuleCatalogApplicationService(
            TriageRuleVersionRepository triageRuleVersionRepository,
            ActiveScenarioChecker activeScenarioChecker,
            LogicalClock logicalClock
    ) {
        this.triageRuleVersionRepository = Objects.requireNonNull(triageRuleVersionRepository);
        this.activeScenarioChecker = Objects.requireNonNull(activeScenarioChecker);
        this.logicalClock = Objects.requireNonNull(logicalClock);
    }

    /**
     * Caso de uso HU-04 (ACT-03): valida que no exista un escenario experimental activo,
     * verifica la completitud y consistencia del catálogo INC-A..INC-D sin duplicados ni omisiones,
     * y publica/activa una nueva versión inmutable RUL-VXX conservando las versiones previas intactas.
     */
    @Transactional
    public TriageRuleVersion publishAndActivateNewVersion(PublishTriageRuleVersionCommand command) {
        if (activeScenarioChecker.isAnyScenarioActiveForUpdate()) {
            throw new EmergencyValidationException(
                    "global",
                    "Operación rechazada: no se permite publicar ni activar versiones de reglas de clasificación durante un escenario experimental activo (RN-EME-012, HU-04 CA-3)."
            );
        }

        if (command == null || command.rules() == null || command.rules().isEmpty()) {
            throw new EmergencyValidationException(
                    "global",
                    "El borrador de reglas a publicar no puede estar vacío; debe definir INC-A, INC-B, INC-C e INC-D (RN-EME-011.5, HU-04 CA-4)."
            );
        }

        List<TriageRuleEntry> parsedEntries = new ArrayList<>();
        for (PublishTriageRuleVersionCommand.RuleDraftItem item : command.rules()) {
            if (item == null) {
                throw new EmergencyValidationException("global", "Las entradas del borrador de reglas no pueden ser nulas.");
            }
            IncidentCategory category = parseCategory(item.incidentCategoryCode());
            SimulatedPriority priority = parsePriority(item.simulatedPriorityCode(), category.code());
            EquipmentType equipment = parseEquipment(item.requiredEquipmentCode(), category.code());
            parsedEntries.add(new TriageRuleEntry(category, priority, equipment, true));
        }

        int versionNumber = triageRuleVersionRepository.nextVersionNumber();
        TriageRuleVersionId versionId = TriageRuleVersionId.of(String.format("RUL-V%02d", versionNumber));

        final TriageRuleVersion newActiveVersion;
        try {
            newActiveVersion = TriageRuleVersion.createCompleteActiveVersion(
                    versionId,
                    versionNumber,
                    logicalClock.currentTime(),
                    parsedEntries
            );
        } catch (IllegalArgumentException ex) {
            throw new EmergencyValidationException("global", ex.getMessage());
        }

        return triageRuleVersionRepository.publishAndActivate(newActiveVersion);
    }

    @Transactional(readOnly = true)
    public Optional<TriageRuleVersion> findActiveVersion() {
        return triageRuleVersionRepository.findActiveVersion();
    }

    @Transactional(readOnly = true)
    public List<TriageRuleVersion> listAllVersions() {
        return triageRuleVersionRepository.findAllOrderByVersionNumberDesc();
    }

    @Transactional(readOnly = true)
    public boolean isAnyScenarioActive() {
        return activeScenarioChecker.isAnyScenarioActive();
    }

    private static IncidentCategory parseCategory(String rawCode) {
        try {
            return IncidentCategory.fromCode(rawCode);
        } catch (IllegalArgumentException ex) {
            throw new EmergencyValidationException("global", ex.getMessage());
        }
    }

    private static SimulatedPriority parsePriority(String rawPriority, String categoryCode) {
        if (rawPriority == null || rawPriority.isBlank()) {
            throw new EmergencyValidationException(
                    "global",
                    "La prioridad simulada para la categoría " + categoryCode
                            + " es obligatoria (HIGH/Alta, MEDIUM/Media o LOW/Baja; RN-EME-011.5)."
            );
        }
        String normalized = rawPriority.trim().toUpperCase();
        return switch (normalized) {
            case "HIGH", "ALTA" -> SimulatedPriority.HIGH;
            case "MEDIUM", "MEDIA" -> SimulatedPriority.MEDIUM;
            case "LOW", "BAJA" -> SimulatedPriority.LOW;
            default -> throw new EmergencyValidationException(
                    "global",
                    "La prioridad simulada '" + rawPriority.trim() + "' para la categoría " + categoryCode
                            + " está fuera del catálogo aprobado {HIGH, MEDIUM, LOW} (HU-04 CA-4)."
            );
        };
    }

    private static EquipmentType parseEquipment(String rawEquipment, String categoryCode) {
        if (rawEquipment == null || rawEquipment.isBlank()) {
            throw new EmergencyValidationException(
                    "global",
                    "El equipamiento requerido para la categoría " + categoryCode
                            + " es obligatorio (BASIC o SPECIALIZED; RN-EME-011.5)."
            );
        }
        String normalized = rawEquipment.trim().toUpperCase();
        return switch (normalized) {
            case "BASIC", "BASICO", "BÁSICO" -> EquipmentType.BASIC;
            case "SPECIALIZED", "ESPECIALIZADO" -> EquipmentType.SPECIALIZED;
            default -> throw new EmergencyValidationException(
                    "global",
                    "El equipamiento requerido '" + rawEquipment.trim() + "' para la categoría " + categoryCode
                            + " está fuera del catálogo aprobado {BASIC, SPECIALIZED} (HU-04 CA-4)."
            );
        };
    }
}
