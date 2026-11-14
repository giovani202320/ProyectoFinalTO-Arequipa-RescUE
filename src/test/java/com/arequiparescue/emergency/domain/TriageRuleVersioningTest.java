package com.arequiparescue.emergency.domain;

import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dominio puro — Versionado inmutable y completitud de TriageRuleVersion (HU-04, RN-EME-011)")
class TriageRuleVersioningTest {

    @Test
    @DisplayName("Crea una nueva versión activa completa con las 4 categorías INC-A a INC-D en orden")
    void shouldCreateCompleteActiveVersion() {
        TriageRuleVersion version = TriageRuleVersion.createCompleteActiveVersion(
                TriageRuleVersionId.of("RUL-V02"),
                2,
                LogicalTime.of(5),
                List.of(
                        new TriageRuleEntry(IncidentCategory.INC_D, SimulatedPriority.MEDIUM, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, true),
                        new TriageRuleEntry(IncidentCategory.INC_C, SimulatedPriority.LOW, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_B, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, true)
                )
        );

        assertThat(version.id().value()).isEqualTo("RUL-V02");
        assertThat(version.versionNumber()).isEqualTo(2);
        assertThat(version.status()).isEqualTo(TriageRuleStatus.ACTIVE);
        assertThat(version.rules()).extracting(r -> r.category().code())
                .containsExactly("INC-A", "INC-B", "INC-C", "INC-D");
    }

    @Test
    @DisplayName("Rechaza un borrador con categorías faltantes o duplicadas (HU-04 CA-4)")
    void shouldRejectIncompleteOrDuplicateCategories() {
        assertThatThrownBy(() -> TriageRuleVersion.createCompleteActiveVersion(
                TriageRuleVersionId.of("RUL-V02"),
                2,
                LogicalTime.of(1),
                List.of(
                        new TriageRuleEntry(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, true),
                        new TriageRuleEntry(IncidentCategory.INC_B, SimulatedPriority.MEDIUM, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_C, SimulatedPriority.LOW, EquipmentType.BASIC, true)
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INC-D");

        assertThatThrownBy(() -> TriageRuleVersion.createCompleteActiveVersion(
                TriageRuleVersionId.of("RUL-V02"),
                2,
                LogicalTime.of(1),
                List.of(
                        new TriageRuleEntry(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, true),
                        new TriageRuleEntry(IncidentCategory.INC_A, SimulatedPriority.MEDIUM, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_C, SimulatedPriority.LOW, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_D, SimulatedPriority.HIGH, EquipmentType.BASIC, true)
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicada");
    }
}
