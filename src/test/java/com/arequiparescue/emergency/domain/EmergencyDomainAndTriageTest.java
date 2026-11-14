package com.arequiparescue.emergency.domain;

import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Emergency & DeterministicTriageService — Pruebas unitarias de dominio puro (HU-01, HU-02, DEC-016, DEC-019)")
class EmergencyDomainAndTriageTest {

    private final DeterministicTriageService triageService = new DeterministicTriageService();
    private final TriageRuleVersion activeCatalog = TriageRuleVersion.initialApprovedCatalog();

    @ParameterizedTest(name = "Categoría {0} -> Prioridad {1}, Equipamiento {2}, Versión {3}")
    @CsvSource({
            "INC-A, HIGH,   Alta,  SPECIALIZED, Especializado, RUL-V01",
            "INC-B, MEDIUM, Media, BASIC,       Básico,        RUL-V01",
            "INC-C, LOW,    Baja,  BASIC,       Básico,        RUL-V01",
            "INC-D, HIGH,   Alta,  BASIC,       Básico,        RUL-V01"
    })
    @DisplayName("Clasifica INC-A, INC-B, INC-C e INC-D exactamente según el catálogo inicial aprobado en DEC-016 (RN-EME-007)")
    void shouldClassifyInitialApprovedCategoriesAccordingToDec016(
            String categoryCode,
            SimulatedPriority expectedPriority,
            String expectedPriorityLabel,
            EquipmentType expectedEquipment,
            String expectedEquipmentLabel,
            String expectedRuleVersionId
    ) {
        TriageClassification result = triageService.classify(categoryCode, activeCatalog);

        assertThat(result.category().code()).isEqualTo(categoryCode);
        assertThat(result.simulatedPriority()).isEqualTo(expectedPriority);
        assertThat(result.simulatedPriority().displayLabel()).isEqualTo(expectedPriorityLabel);
        assertThat(result.requiredEquipment()).isEqualTo(expectedEquipment);
        assertThat(result.requiredEquipment().displayLabel()).isEqualTo(expectedEquipmentLabel);
        assertThat(result.appliedRuleVersionId().value()).isEqualTo(expectedRuleVersionId);
    }

    @Test
    @DisplayName("Garantiza el determinismo del triage ante invocaciones repetidas con la misma entrada y versión de reglas (RN-EME-003)")
    void shouldProduceIdenticalClassificationAcrossRepeatedEvaluations() {
        for (String categoryCode : List.of("INC-A", "INC-B", "INC-C", "INC-D")) {
            TriageClassification first = triageService.classify(categoryCode, activeCatalog);
            for (int i = 0; i < 25; i++) {
                TriageClassification subsequent = triageService.classify(categoryCode, activeCatalog);
                assertThat(subsequent).isEqualTo(first);
            }
        }
    }

    @Test
    @DisplayName("Crea la emergencia en estado inicial REGISTERED con tiempo lógico t >= 0 y permite descripción vacía o con texto (HU-01 CA-1, RN-EME-009, RN-EME-010.4)")
    void shouldRegisterEmergencyInInitialStateRegisteredWithLogicalTimeAndOptionalDescription() {
        TriageClassification classification = triageService.classify("INC-A", activeCatalog);

        Emergency emptyDescEmergency = Emergency.register(
                EmergencyId.fromSequence(1L),
                NodeId.of("N01"),
                "   ",
                classification,
                LogicalTime.of(0L)
        );

        Emergency textDescEmergency = Emergency.register(
                EmergencyId.fromSequence(2L),
                NodeId.of("N07"),
                "Colisión vehicular simulada en intersección académica",
                classification,
                LogicalTime.of(15L)
        );

        assertThat(emptyDescEmergency.id().value()).isEqualTo("EMG-00001");
        assertThat(emptyDescEmergency.state()).isEqualTo(EmergencyState.REGISTERED);
        assertThat(emptyDescEmergency.description()).isEmpty();
        assertThat(emptyDescEmergency.registeredAt()).isEqualTo(LogicalTime.ZERO);
        assertThat(emptyDescEmergency.simulatedPriority()).isEqualTo(SimulatedPriority.HIGH);
        assertThat(emptyDescEmergency.requiredEquipment()).isEqualTo(EquipmentType.SPECIALIZED);
        assertThat(emptyDescEmergency.appliedRuleVersionId().value()).isEqualTo("RUL-V01");
        assertThat(emptyDescEmergency.assignedAmbulanceId()).isNull();
        assertThat(emptyDescEmergency.reservedOrOccupiedHospitalId()).isNull();

        assertThat(textDescEmergency.id().value()).isEqualTo("EMG-00002");
        assertThat(textDescEmergency.state()).isEqualTo(EmergencyState.REGISTERED);
        assertThat(textDescEmergency.locationNode()).isEqualTo(NodeId.of("N07"));
        assertThat(textDescEmergency.description()).isEqualTo("Colisión vehicular simulada en intersección académica");
        assertThat(textDescEmergency.registeredAt().ticks()).isEqualTo(15L);
    }

    @Test
    @DisplayName("Rechaza entradas inválidas: categoría vacía/inexistente/inactiva, nodo fuera de N01-N10 o versión no activa sin aplicar valores por defecto (HU-01 CA-2, RN-EME-010)")
    void shouldRejectInvalidCategoryInactiveRuleInvalidNodeOrNonActiveVersionWithoutDefaults() {
        assertThatThrownBy(() -> triageService.classify("", activeCatalog))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoria");

        assertThatThrownBy(() -> triageService.classify("INC-Z", activeCatalog))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");

        assertThatThrownBy(() -> NodeId.of("N99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("N01..N10");

        TriageRuleVersion catalogWithInactiveRule = new TriageRuleVersion(
                TriageRuleVersionId.of("RUL-V02"),
                2,
                TriageRuleStatus.ACTIVE,
                LogicalTime.of(5L),
                List.of(new TriageRuleEntry(
                        IncidentCategory.INC_A,
                        SimulatedPriority.HIGH,
                        EquipmentType.SPECIALIZED,
                        false
                ))
        );

        assertThatThrownBy(() -> triageService.classify("INC-A", catalogWithInactiveRule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no se aplican clasificaciones por defecto");

        TriageRuleVersion draftVersion = new TriageRuleVersion(
                TriageRuleVersionId.of("RUL-V03"),
                3,
                TriageRuleStatus.DRAFT,
                LogicalTime.of(10L),
                activeCatalog.rules()
        );

        assertThatThrownBy(() -> triageService.classify("INC-B", draftVersion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("Conserva exclusivamente los 7 estados aprobados por DEC-019 y soporta el seguimiento de WAITING_HOSPITAL y COMPLETED (HU-02 CA-1, CA-2, CA-3)")
    void shouldExposeOnlySevenApprovedStatesAndSupportTrackingInvariants() {
        List<String> stateNames = Arrays.stream(EmergencyState.values()).map(Enum::name).toList();
        assertThat(stateNames).containsExactly(
                "REGISTERED",
                "WAITING_AMBULANCE",
                "ASSIGNED",
                "AT_INCIDENT",
                "WAITING_HOSPITAL",
                "IN_TRANSPORT",
                "COMPLETED"
        );

        Emergency waitingHospital = Emergency.reconstruct(
                EmergencyId.of("EMG-00010"),
                IncidentCategory.INC_A,
                NodeId.of("N03"),
                "Paciente simulado en espera de plaza hospitalaria",
                SimulatedPriority.HIGH,
                EquipmentType.SPECIALIZED,
                TriageRuleVersionId.of("RUL-V01"),
                EmergencyState.WAITING_HOSPITAL,
                "AMB-01",
                null,
                LogicalTime.of(0L),
                null,
                LogicalTime.of(1L),
                LogicalTime.of(4L),
                LogicalTime.of(4L),
                null,
                null,
                "HOSPITAL_RESERVATION_RETRY_EXHAUSTED"
        );
        assertThat(waitingHospital.retainsAmbulanceAtIncidentWaitingHospital()).isTrue();
        assertThat(waitingHospital.isCycleClosed()).isFalse();

        Emergency completed = Emergency.reconstruct(
                EmergencyId.of("EMG-00011"),
                IncidentCategory.INC_B,
                NodeId.of("N02"),
                "Traslado completado",
                SimulatedPriority.MEDIUM,
                EquipmentType.BASIC,
                TriageRuleVersionId.of("RUL-V01"),
                EmergencyState.COMPLETED,
                null,
                "HOSP-01",
                LogicalTime.of(2L),
                null,
                LogicalTime.of(2L),
                LogicalTime.of(5L),
                null,
                LogicalTime.of(5L),
                LogicalTime.of(9L),
                null
        );
        assertThat(completed.isCycleClosed()).isTrue();
        assertThat(completed.completedAt()).isEqualTo(LogicalTime.of(9L));
    }

    @Test
    @DisplayName("Verifica pureza arquitectónica: ninguna clase de emergency.domain importa Spring, JPA, Hibernate o Servlet (DEC-003, ADR-0001 Secc. 4.2)")
    void shouldKeepEmergencyDomainFreeOfFrameworkAndPersistenceImports() throws IOException {
        Path domainDir = Path.of("src/main/java/com/arequiparescue/emergency/domain");
        try (Stream<Path> paths = Files.walk(domainDir)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source)
                        .as("Archivo de dominio %s no debe importar frameworks", file.getFileName())
                        .doesNotContain("org.springframework.")
                        .doesNotContain("jakarta.persistence.")
                        .doesNotContain("org.hibernate.")
                        .doesNotContain("jakarta.servlet.")
                        .doesNotContain("jakarta.validation.");
            }
        }
    }
}
