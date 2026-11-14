package com.arequiparescue.emergency.application;

import java.util.List;

/**
 * Comando de aplicación para publicar y activar una nueva versión inmutable del catálogo
 * de reglas de clasificación (HU-04, DEC-010, DEC-016, RN-EME-011, RN-EME-012).
 */
public record PublishTriageRuleVersionCommand(
        List<RuleDraftItem> rules
) {

    public record RuleDraftItem(
            String incidentCategoryCode,
            String simulatedPriorityCode,
            String requiredEquipmentCode
    ) {
    }
}
