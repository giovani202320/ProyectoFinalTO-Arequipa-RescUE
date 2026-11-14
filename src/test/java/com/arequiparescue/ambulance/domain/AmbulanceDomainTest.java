package com.arequiparescue.ambulance.domain;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dominio puro — Ambulance, AmbulanceState, INV-SM-01 e INV-SM-02 (HU-03)")
class AmbulanceDomainTest {

    @Test
    @DisplayName("Mantiene exactamente los 4 estados aprobados por DEC-019 (INV-SM-01)")
    void shouldContainExactlyTheFourApprovedStates() {
        assertThat(AmbulanceState.values()).containsExactlyInAnyOrder(
                AmbulanceState.AVAILABLE,
                AmbulanceState.ASSIGNED,
                AmbulanceState.AT_INCIDENT,
                AmbulanceState.TRANSPORTING
        );
    }

    @Test
    @DisplayName("Registra una ambulancia en el catálogo en estado AVAILABLE sin emergencia vinculada")
    void shouldRegisterAmbulanceInAvailableState() {
        Ambulance ambulance = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"),
                EquipmentType.SPECIALIZED,
                NodeId.of("N03")
        );

        assertThat(ambulance.id().value()).isEqualTo("AMB-01");
        assertThat(ambulance.equipmentType()).isEqualTo(EquipmentType.SPECIALIZED);
        assertThat(ambulance.currentNode().value()).isEqualTo("N03");
        assertThat(ambulance.state()).isEqualTo(AmbulanceState.AVAILABLE);
        assertThat(ambulance.assignedEmergencyId()).isNull();
    }

    @Test
    @DisplayName("Rechaza violaciones de acoplamiento INV-SM-02 entre estado y emergencia asignada")
    void shouldRejectInvalidStateEmergencyBinding() {
        assertThatThrownBy(() -> Ambulance.reconstruct(
                AmbulanceId.of("AMB-01"),
                EquipmentType.BASIC,
                NodeId.of("N01"),
                AmbulanceState.AVAILABLE,
                EmergencyId.of("EMG-00001")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INV-SM-02");

        assertThatThrownBy(() -> Ambulance.reconstruct(
                AmbulanceId.of("AMB-02"),
                EquipmentType.BASIC,
                NodeId.of("N01"),
                AmbulanceState.ASSIGNED,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INV-SM-02");
    }

    @Test
    @DisplayName("Bloquea la actualización del catálogo cuando existe un escenario experimental activo (HU-03 CA-1)")
    void shouldBlockUpdateWhenScenarioIsActive() {
        Ambulance ambulance = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"),
                EquipmentType.BASIC,
                NodeId.of("N01")
        );

        assertThatThrownBy(() -> ambulance.updateCatalogConfiguration(
                EquipmentType.SPECIALIZED,
                NodeId.of("N02"),
                true
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("escenario experimental está activo");
    }

    @Test
    @DisplayName("Permite actualizar equipamiento y nodo fuera de escenarios activos si está AVAILABLE")
    void shouldUpdateConfigurationWhenScenarioIsInactiveAndAvailable() {
        Ambulance ambulance = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"),
                EquipmentType.BASIC,
                NodeId.of("N01")
        );

        Ambulance updated = ambulance.updateCatalogConfiguration(
                EquipmentType.SPECIALIZED,
                NodeId.of("N05"),
                false
        );

        assertThat(updated.equipmentType()).isEqualTo(EquipmentType.SPECIALIZED);
        assertThat(updated.currentNode().value()).isEqualTo("N05");
        assertThat(updated.state()).isEqualTo(AmbulanceState.AVAILABLE);
    }

    @Test
    @DisplayName("Verifica que las clases de dominio de ambulancias no dependen de Spring ni JPA/Hibernate")
    void shouldKeepDomainPureWithoutFrameworkAnnotations() {
        Annotation[] annotations = Ambulance.class.getAnnotations();
        assertThat(Arrays.stream(annotations)
                .map(a -> a.annotationType().getPackageName()))
                .noneMatch(pkg -> pkg.startsWith("jakarta.persistence")
                        || pkg.startsWith("org.springframework")
                        || pkg.startsWith("org.hibernate"));
    }
}
