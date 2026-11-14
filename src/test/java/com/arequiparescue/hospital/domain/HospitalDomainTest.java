package com.arequiparescue.hospital.domain;

import com.arequiparescue.routing.domain.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dominio puro — Hospital e invariantes C, R, O, D sin especialidades (HU-03, DEC-018)")
class HospitalDomainTest {

    @Test
    @DisplayName("Registra un hospital sin especialidades con R = 0, O = 0 y D = C (HU-03 CA-1, RN-HOS-004)")
    void shouldRegisterHospitalWithValidNumericCapacity() {
        Hospital hospital = Hospital.registerInCatalog(
                HospitalId.of("HOSP-01"),
                "Hospital Central Simulado",
                NodeId.of("N02"),
                true,
                12
        );

        assertThat(hospital.id().value()).isEqualTo("HOSP-01");
        assertThat(hospital.locationNode().value()).isEqualTo("N02");
        assertThat(hospital.enabled()).isTrue();
        assertThat(hospital.totalCapacity()).isEqualTo(12);
        assertThat(hospital.reservedSlots()).isZero();
        assertThat(hospital.occupiedSlots()).isZero();
        assertThat(hospital.availableSlots()).isEqualTo(12);
    }

    @Test
    @DisplayName("Rechaza fijar C < R + O cuando existen plazas reservadas u ocupadas (HU-03 CA-3, CA-HOS-12)")
    void shouldRejectCapacityBelowCommittedSlots() {
        Hospital hospital = Hospital.reconstruct(
                HospitalId.of("HOSP-01"),
                "Hospital Norte",
                NodeId.of("N04"),
                true,
                10,
                2,
                3
        );

        assertThat(hospital.availableSlots()).isEqualTo(5);

        assertThatThrownBy(() -> hospital.updateCatalogConfiguration(
                "Hospital Norte",
                NodeId.of("N04"),
                true,
                4,
                false
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("R + O = 5");
    }

    @Test
    @DisplayName("Rechaza modificar capacidad total C durante un escenario experimental activo (HU-03 CA-2, RN-HOS-015)")
    void shouldRejectCapacityChangeDuringActiveScenario() {
        Hospital hospital = Hospital.registerInCatalog(
                HospitalId.of("HOSP-01"),
                "Hospital Sur",
                NodeId.of("N06"),
                true,
                8
        );

        assertThatThrownBy(() -> hospital.updateCatalogConfiguration(
                "Hospital Sur",
                NodeId.of("N06"),
                true,
                10,
                true
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RN-HOS-015");
    }

    @Test
    @DisplayName("Rechaza capacidades negativas C < 0, R < 0 u O < 0")
    void shouldRejectNegativeCapacityValues() {
        assertThatThrownBy(() -> Hospital.registerInCatalog(
                HospitalId.of("HOSP-01"),
                "Hospital Inválido",
                NodeId.of("N01"),
                true,
                -1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("C >= 0");
    }

    @Test
    @DisplayName("Verifica que Hospital no depende de Spring, JPA ni Hibernate")
    void shouldKeepHospitalDomainPure() {
        Annotation[] annotations = Hospital.class.getAnnotations();
        assertThat(Arrays.stream(annotations)
                .map(a -> a.annotationType().getPackageName()))
                .noneMatch(pkg -> pkg.startsWith("jakarta.persistence")
                        || pkg.startsWith("org.springframework")
                        || pkg.startsWith("org.hibernate"));
    }
}
