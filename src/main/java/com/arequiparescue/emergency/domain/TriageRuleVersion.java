package com.arequiparescue.emergency.domain;

import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agregado inmutable del dominio que representa una versión del catálogo de reglas de clasificación
 * simulada (DEC-010, DEC-016, RN-EME-002 a RN-EME-011).
 */
public final class TriageRuleVersion {

    private final TriageRuleVersionId id;
    private final int versionNumber;
    private final TriageRuleStatus status;
    private final LogicalTime publishedAt;
    private final Map<IncidentCategory, TriageRuleEntry> rulesByCategory;

    public TriageRuleVersion(
            TriageRuleVersionId id,
            int versionNumber,
            TriageRuleStatus status,
            LogicalTime publishedAt,
            Collection<TriageRuleEntry> rules
    ) {
        this.id = Objects.requireNonNull(id, "El identificador de la versión de reglas no puede ser nulo.");
        if (versionNumber <= 0) {
            throw new IllegalArgumentException(
                    "El número de versión de reglas debe ser positivo (> 0). Valor recibido: " + versionNumber);
        }
        this.versionNumber = versionNumber;
        this.status = Objects.requireNonNull(status, "El estado de la versión de reglas no puede ser nulo.");
        this.publishedAt = Objects.requireNonNull(
                publishedAt, "El instante lógico de publicación de la versión de reglas no puede ser nulo.");
        Objects.requireNonNull(rules, "La colección de reglas de la versión no puede ser nula.");
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Una versión de reglas de clasificación no puede estar vacía (RN-EME-011).");
        }

        EnumMap<IncidentCategory, TriageRuleEntry> map = new EnumMap<>(IncidentCategory.class);
        for (TriageRuleEntry entry : rules) {
            Objects.requireNonNull(entry, "Cada entrada de regla de clasificación debe ser no nula.");
            TriageRuleEntry previous = map.put(entry.category(), entry);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "No se permiten categorías duplicadas en una misma versión de reglas (RN-EME-011.5): "
                                + entry.category().code());
            }
        }
        this.rulesByCategory = Collections.unmodifiableMap(map);
    }

    /**
     * Construye el catálogo inicial aprobado por DEC-016 y RN-EME-007 (versión RUL-V01 activa en t=0).
     */
    public static TriageRuleVersion initialApprovedCatalog() {
        return new TriageRuleVersion(
                TriageRuleVersionId.of("RUL-V01"),
                1,
                TriageRuleStatus.ACTIVE,
                LogicalTime.ZERO,
                List.of(
                        new TriageRuleEntry(
                                IncidentCategory.INC_A,
                                SimulatedPriority.HIGH,
                                EquipmentType.SPECIALIZED,
                                true
                        ),
                        new TriageRuleEntry(
                                IncidentCategory.INC_B,
                                SimulatedPriority.MEDIUM,
                                EquipmentType.BASIC,
                                true
                        ),
                        new TriageRuleEntry(
                                IncidentCategory.INC_C,
                                SimulatedPriority.LOW,
                                EquipmentType.BASIC,
                                true
                        ),
                        new TriageRuleEntry(
                                IncidentCategory.INC_D,
                                SimulatedPriority.HIGH,
                                EquipmentType.BASIC,
                                true
                        )
                )
        );
    }

    /**
     * Evalúa de forma determinista y sin valores por defecto la categoría del incidente contra esta versión
     * activa de reglas (RN-EME-003, RN-EME-004, RN-EME-007, RN-EME-010).
     */
    public TriageClassification classify(IncidentCategory category) {
        Objects.requireNonNull(category, "La categoría del incidente a clasificar es obligatoria (RN-EME-010.1).");
        if (this.status != TriageRuleStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Solo una versión de reglas en estado ACTIVE puede clasificar nuevos registros (RN-EME-011.1). Versión: "
                            + this.id + " (" + this.status + ")");
        }
        TriageRuleEntry entry = this.rulesByCategory.get(category);
        if (entry == null || !entry.active()) {
            throw new IllegalArgumentException(
                    "No existe una regla activa y válida para la categoría " + category.code()
                            + " en la versión " + this.id
                            + "; no se aplican clasificaciones por defecto (RN-EME-010.2, RN-EME-010.5).");
        }
        return new TriageClassification(
                category,
                entry.simulatedPriority(),
                entry.requiredEquipment(),
                this.id
        );
    }

    public TriageRuleVersionId id() {
        return id;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public TriageRuleStatus status() {
        return status;
    }

    public LogicalTime publishedAt() {
        return publishedAt;
    }

    public List<TriageRuleEntry> rules() {
        return List.copyOf(rulesByCategory.values());
    }
}
