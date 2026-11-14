package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.AmbulanceCatalogValidationException;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.application.UpdateAmbulanceCommand;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.infrastructure.persistence.SpringDataAmbulanceRepository;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.PublishTriageRuleVersionCommand;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.application.TriageRuleCatalogApplicationService;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleStatus;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalCatalogValidationException;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.application.UpdateHospitalCommand;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.infrastructure.persistence.HospitalJpaEntity;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.simulation.infrastructure.persistence.SimulationScenarioJpaEntity;
import com.arequiparescue.simulation.infrastructure.persistence.SpringDataSimulationScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración contra MySQL 8.4 LTS que valida la migración Flyway V4, los catálogos
 * de ambulancias y hospitales, las invariantes C/R/O/D, el bloqueo durante escenarios activos,
 * el versionado inmutable de reglas (RUL-V01 -> RUL-V02) y la topología N01–N10 (HU-03 y HU-04).
 */
@SpringBootTest
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Flyway V4 + Catálogos HU-03 y Versionado de Reglas HU-04")
class CatalogsAndRulesMySqlIntegrationTest {

    @Autowired
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @Autowired
    private HospitalCatalogApplicationService hospitalCatalogService;

    @Autowired
    private TriageRuleCatalogApplicationService triageRuleCatalogService;

    @Autowired
    private EmergencyApplicationService emergencyApplicationService;

    @Autowired
    private RoadGraphRepository roadGraphRepository;

    @Autowired
    private SpringDataAmbulanceRepository springDataAmbulanceRepository;

    @Autowired
    private SpringDataHospitalRepository springDataHospitalRepository;

    @Autowired
    private SpringDataSimulationScenarioRepository springDataSimulationScenarioRepository;

    @Autowired
    private SpringDataEmergencyRepository springDataEmergencyRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabaseState() {
        springDataAmbulanceRepository.deleteAll();
        springDataEmergencyRepository.deleteAll();
        springDataHospitalRepository.deleteAll();
        springDataSimulationScenarioRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM triage_rules WHERE rule_version_id <> 'RUL-V01'");
        jdbcTemplate.update("DELETE FROM triage_rule_versions WHERE id <> 'RUL-V01'");
        jdbcTemplate.update("UPDATE triage_rule_versions SET status = 'ACTIVE' WHERE id = 'RUL-V01'");

        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("HOSPITAL", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("TRIAGE_RULE_VERSION", 2L));
    }

    @Test
    @DisplayName("Flyway V4 aplicada correctamente con tablas y secuencias iniciales")
    void shouldVerifyFlywayV4MigrationApplied() {
        Integer v4Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '4' AND success = 1",
                Integer.class
        );
        assertThat(v4Count).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-03 CA-1 y CA-3: registra ambulancias y hospitales, valida C >= R + O y consulta topología con N10 aislado")
    void shouldManageAmbulancesHospitalsAndTopologyInMySql() {
        Ambulance createdAmb = ambulanceCatalogService.registerAmbulance(
                new RegisterAmbulanceCommand(null, "SPECIALIZED", "N02")
        );
        assertThat(createdAmb.id().value()).isEqualTo("AMB-01");
        assertThat(createdAmb.state()).isEqualTo(AmbulanceState.AVAILABLE);

        Ambulance updatedAmb = ambulanceCatalogService.updateAmbulance(
                new UpdateAmbulanceCommand("AMB-01", "BASIC", "N04")
        );
        assertThat(updatedAmb.equipmentType()).isEqualTo(EquipmentType.BASIC);
        assertThat(updatedAmb.currentNode().value()).isEqualTo("N04");

        Hospital createdHosp = hospitalCatalogService.registerHospital(
                new RegisterHospitalCommand(null, "Hospital Regional Simulado", "N03", true, 10)
        );
        assertThat(createdHosp.id().value()).isEqualTo("HOSP-01");
        assertThat(createdHosp.availableSlots()).isEqualTo(10);

        springDataHospitalRepository.saveAndFlush(
                new HospitalJpaEntity("HOSP-02", "Hospital Comprometido", "N05", true, 10, 3, 2)
        );

        assertThatThrownBy(() -> hospitalCatalogService.updateHospital(
                new UpdateHospitalCommand("HOSP-02", "Hospital Comprometido", "N05", true, 4)
        )).isInstanceOf(HospitalCatalogValidationException.class)
                .hasMessageContaining("R + O = 5");

        RoadGraph graph = roadGraphRepository.loadGraph();
        assertThat(graph.nodeCount()).isEqualTo(10);
        assertThat(graph.isIsolated(NodeId.of("N10"))).isTrue();
    }

    @Test
    @DisplayName("HU-03 CA-2 y HU-04 CA-3: bloquea catálogos, capacidad C y publicación de reglas durante escenario ACTIVE")
    void shouldBlockCatalogsAndRulesDuringActiveScenario() {
        hospitalCatalogService.registerHospital(
                new RegisterHospitalCommand("HOSP-01", "Hospital Base", "N01", true, 8)
        );
        springDataSimulationScenarioRepository.saveAndFlush(
                new SimulationScenarioJpaEntity("SCN-01", "Escenario Activo Test", "ACTIVE", 0L)
        );

        assertThatThrownBy(() -> ambulanceCatalogService.registerAmbulance(
                new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01")
        )).isInstanceOf(AmbulanceCatalogValidationException.class)
                .hasMessageContaining("escenario experimental está activo");

        assertThatThrownBy(() -> hospitalCatalogService.updateHospital(
                new UpdateHospitalCommand("HOSP-01", "Hospital Base", "N01", true, 12)
        )).isInstanceOf(HospitalCatalogValidationException.class)
                .hasMessageContaining("RN-HOS-015");

        assertThatThrownBy(() -> triageRuleCatalogService.publishAndActivateNewVersion(
                new PublishTriageRuleVersionCommand(List.of(
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-A", "HIGH", "SPECIALIZED"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-B", "MEDIUM", "BASIC"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-C", "LOW", "BASIC"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-D", "HIGH", "BASIC")
                ))
        )).isInstanceOf(EmergencyValidationException.class)
                .hasMessageContaining("RN-EME-012");
    }

    @Test
    @DisplayName("HU-04 CA-1 y CA-2: publica RUL-V02 inmutable y preserva la clasificación y RUL-V01 en emergencias previas")
    void shouldPublishNewRuleVersionWithoutAlteringPreviousEmergencies() {
        Emergency priorEmergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Emergencia bajo RUL-V01")
        );
        assertThat(priorEmergency.appliedRuleVersionId().value()).isEqualTo("RUL-V01");
        assertThat(priorEmergency.simulatedPriority()).isEqualTo(SimulatedPriority.MEDIUM);
        assertThat(priorEmergency.requiredEquipment()).isEqualTo(EquipmentType.BASIC);

        TriageRuleVersion newVersion = triageRuleCatalogService.publishAndActivateNewVersion(
                new PublishTriageRuleVersionCommand(List.of(
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-A", "HIGH", "SPECIALIZED"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-B", "HIGH", "SPECIALIZED"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-C", "LOW", "BASIC"),
                        new PublishTriageRuleVersionCommand.RuleDraftItem("INC-D", "MEDIUM", "BASIC")
                ))
        );

        assertThat(newVersion.id().value()).isEqualTo("RUL-V02");
        assertThat(newVersion.status()).isEqualTo(TriageRuleStatus.ACTIVE);

        List<TriageRuleVersion> history = triageRuleCatalogService.listAllVersions();
        assertThat(history).hasSize(2);
        assertThat(history.stream().filter(v -> v.id().value().equals("RUL-V01")).findFirst().orElseThrow().status())
                .isEqualTo(TriageRuleStatus.PUBLISHED);

        Emergency newEmergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N03", "Emergencia bajo RUL-V02")
        );
        assertThat(newEmergency.appliedRuleVersionId().value()).isEqualTo("RUL-V02");
        assertThat(newEmergency.simulatedPriority()).isEqualTo(SimulatedPriority.HIGH);
        assertThat(newEmergency.requiredEquipment()).isEqualTo(EquipmentType.SPECIALIZED);

        Emergency reloadedPrior = emergencyApplicationService.getEmergencyById(priorEmergency.id().value());
        assertThat(reloadedPrior.appliedRuleVersionId().value()).isEqualTo("RUL-V01");
        assertThat(reloadedPrior.simulatedPriority()).isEqualTo(SimulatedPriority.MEDIUM);
        assertThat(reloadedPrior.requiredEquipment()).isEqualTo(EquipmentType.BASIC);
    }
}
