package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.domain.TriageRuleEntry;
import com.arequiparescue.emergency.domain.TriageRuleVersion;

import java.util.List;

/**
 * DTO de presentación para visualizar una versión inmutable de reglas de clasificación y su tabla
 * de correspondencia INC-A a INC-D (HU-04).
 */
public record TriageRuleVersionView(
        String id,
        int versionNumber,
        String statusCode,
        long publishedAtTicks,
        List<TriageRuleRowView> rules
) {

    public record TriageRuleRowView(
            String categoryCode,
            String categoryLabel,
            String priorityCode,
            String priorityLabel,
            String equipmentCode,
            String equipmentLabel,
            boolean active
    ) {
        public static TriageRuleRowView fromDomain(TriageRuleEntry entry) {
            return new TriageRuleRowView(
                    entry.category().code(),
                    entry.category().displayLabel(),
                    entry.simulatedPriority().name(),
                    entry.simulatedPriority().displayLabel(),
                    entry.requiredEquipment().name(),
                    entry.requiredEquipment().displayLabel(),
                    entry.active()
            );
        }
    }

    public static TriageRuleVersionView fromDomain(TriageRuleVersion version) {
        return new TriageRuleVersionView(
                version.id().value(),
                version.versionNumber(),
                version.status().name(),
                version.publishedAt().ticks(),
                version.rules().stream().map(TriageRuleRowView::fromDomain).toList()
        );
    }
}
