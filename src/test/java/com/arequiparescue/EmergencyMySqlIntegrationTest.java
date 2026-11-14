package com.arequiparescue;

import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleStatus;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.port.TriageRuleVersionRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencyJpaAdapter;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración contra MySQL 8.4 LTS que valida la migración Flyway V3, la versión semilla RUL-V01,
 * la persistencia real de emergencias y la generación concurrente de identificadores EMG-XXXXX bajo
 * bloqueo pesimista SELECT ... FOR UPDATE (HU-01, HU-02, ADR-0002).
 */
@SpringBootTest
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Flyway V3 + Registro, Consulta y Secuencia Concurrente de Emergencias (HU-01 y HU-02)")
class EmergencyMySqlIntegrationTest {

    @Autowired
    private TriageRuleVersionRepository triageRuleVersionRepository;

    @Autowired
    private EmergencyApplicationService emergencyApplicationService;

    @Autowired
    private SpringDataEmergencyRepository springDataEmergencyRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    @BeforeEach
    void resetDatabaseState() {
        springDataEmergencyRepository.deleteAll();
        springDataEmergencySequenceRepository.saveAndFlush(
                new EmergencySequenceJpaEntity(EmergencyJpaAdapter.EMERGENCY_SEQUENCE_NAME, 1L)
        );
    }

    @AfterEach
    void cleanUpEmergencies() {
        springDataEmergencyRepository.deleteAll();
        springDataEmergencySequenceRepository.saveAndFlush(
                new EmergencySequenceJpaEntity(EmergencyJpaAdapter.EMERGENCY_SEQUENCE_NAME, 1L)
        );
    }

    @Test
    @DisplayName("Carga desde MySQL 8.4 la versión activa inicial RUL-V01 sembrada por Flyway V3 con las 4 reglas de DEC-016")
    void shouldLoadSeededActiveTriageRuleVersionFromMySql() {
        TriageRuleVersion activeVersion = triageRuleVersionRepository.findActiveVersion().orElseThrow();

        assertThat(activeVersion.id().value()).isEqualTo("RUL-V01");
        assertThat(activeVersion.versionNumber()).isEqualTo(1);
        assertThat(activeVersion.status()).isEqualTo(TriageRuleStatus.ACTIVE);
        assertThat(activeVersion.rules()).hasSize(4);
    }

    @Test
    @DisplayName("Persiste y consulta en MySQL 8.4 emergencias INC-A, INC-B, INC-C e INC-D en estado REGISTERED y rechaza entradas inválidas")
    void shouldPersistAndRetrieveEmergenciesInMySqlAndRejectInvalidInputs() {
        Emergency incA = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-A", "N01", "Incidente crítico simulado en Cercado"));
        Emergency incB = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N04", ""));
        Emergency incC = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-C", "N07", "Incidente leve simulado"));
        Emergency incD = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-D", "N10", "Incidente en nodo aislado N10"));

        assertThat(incA.id().value()).isEqualTo("EMG-00001");
        assertThat(incA.simulatedPriority()).isEqualTo(SimulatedPriority.HIGH);
        assertThat(incA.requiredEquipment()).isEqualTo(EquipmentType.SPECIALIZED);
        assertThat(incA.state()).isEqualTo(EmergencyState.REGISTERED);
        assertThat(incA.appliedRuleVersionId().value()).isEqualTo("RUL-V01");

        assertThat(incB.id().value()).isEqualTo("EMG-00002");
        assertThat(incB.simulatedPriority()).isEqualTo(SimulatedPriority.MEDIUM);
        assertThat(incB.requiredEquipment()).isEqualTo(EquipmentType.BASIC);
        assertThat(incB.description()).isEmpty();

        assertThat(incC.id().value()).isEqualTo("EMG-00003");
        assertThat(incC.simulatedPriority()).isEqualTo(SimulatedPriority.LOW);
        assertThat(incC.requiredEquipment()).isEqualTo(EquipmentType.BASIC);

        assertThat(incD.id().value()).isEqualTo("EMG-00004");
        assertThat(incD.simulatedPriority()).isEqualTo(SimulatedPriority.HIGH);
        assertThat(incD.requiredEquipment()).isEqualTo(EquipmentType.BASIC);

        List<Emergency> persisted = emergencyApplicationService.listEmergencies();
        assertThat(persisted).hasSize(4);

        Emergency fetched = emergencyApplicationService.getEmergencyById("EMG-00001");
        assertThat(fetched.id().value()).isEqualTo("EMG-00001");
        assertThat(fetched.description()).isEqualTo("Incidente crítico simulado en Cercado");

        assertThatThrownBy(() -> emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-INVALID", "N01", "No debe persistirse")))
                .isInstanceOf(EmergencyValidationException.class);

        assertThat(emergencyApplicationService.listEmergencies()).hasSize(4);
    }

    @Test
    @DisplayName("Garantiza que registros concurrentes simultáneos en MySQL 8.4 generen identificadores EMG-XXXXX únicos sin colisión (ADR-0002)")
    void shouldGenerateDistinctIdentifiersUnderConcurrentRegistrations() throws Exception {
        int concurrentRegistrations = 8;
        CountDownLatch readyLatch = new CountDownLatch(concurrentRegistrations);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRegistrations);

        try {
            List<Callable<Emergency>> tasks = new ArrayList<>();
            String[] categories = {"INC-A", "INC-B", "INC-C", "INC-D"};
            for (int i = 0; i < concurrentRegistrations; i++) {
                final int index = i;
                tasks.add(() -> {
                    readyLatch.countDown();
                    assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
                    String category = categories[index % categories.length];
                    String node = String.format("N%02d", (index % 9) + 1);
                    return emergencyApplicationService.registerEmergency(
                            new RegisterEmergencyCommand(category, node, "Registro concurrente #" + index)
                    );
                });
            }

            List<Future<Emergency>> futures = new ArrayList<>();
            for (Callable<Emergency> task : tasks) {
                futures.add(executor.submit(task));
            }

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<Emergency> results = new ArrayList<>();
            for (Future<Emergency> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            Set<String> generatedIds = results.stream()
                    .map(e -> e.id().value())
                    .collect(Collectors.toSet());

            assertThat(generatedIds).hasSize(concurrentRegistrations);
            assertThat(generatedIds).containsExactlyInAnyOrder(
                    "EMG-00001",
                    "EMG-00002",
                    "EMG-00003",
                    "EMG-00004",
                    "EMG-00005",
                    "EMG-00006",
                    "EMG-00007",
                    "EMG-00008"
            );
            assertThat(emergencyApplicationService.listEmergencies()).hasSize(concurrentRegistrations);
        } finally {
            executor.shutdownNow();
        }
    }
}
