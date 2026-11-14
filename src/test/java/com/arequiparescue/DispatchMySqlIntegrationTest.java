package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.ambulance.infrastructure.persistence.SpringDataAmbulanceRepository;
import com.arequiparescue.dispatch.application.AutomaticDispatchResult;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.SupervisedDispatchResult;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataExperimentOperationResultRepository;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataOperationAttemptLogRepository;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.application.WaitingQueueSimulationTrigger;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración contra MySQL 8.4 LTS que valida Flyway V5, elegibilidad Dijkstra (HU-05),
 * asignación supervisada libre (HU-06 CA-1), prevención de doble asignación concurrente con
 * SELECT ... FOR UPDATE, recarga de alternativas y transición a WAITING_AMBULANCE (HU-06 CA-2 y CA-3).
 */
@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Flyway V5 + Despacho Supervisado Concurrente (HU-05 y HU-06)")
class DispatchMySqlIntegrationTest {

    @Autowired
    private EmergencyApplicationService emergencyApplicationService;

    @Autowired
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @Autowired
    private DispatchApplicationService dispatchApplicationService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private SpringDataOperationAttemptLogRepository attemptLogRepository;

    @Autowired
    private SpringDataExperimentOperationResultRepository operationResultRepository;

    @Autowired
    private SpringDataAmbulanceRepository springDataAmbulanceRepository;

    @Autowired
    private SpringDataEmergencyRepository springDataEmergencyRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    @Autowired
    private WaitingQueueSimulationTrigger waitingQueueSimulationTrigger;

    @Autowired
    private LogicalClock logicalClock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabaseState() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        attemptLogRepository.deleteAll();
        operationResultRepository.deleteAll();
        springDataAmbulanceRepository.deleteAll();
        springDataEmergencyRepository.deleteAll();

        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
    }

    @Test
    @DisplayName("Flyway V5 aplicada correctamente con tablas de trazabilidad")
    void shouldVerifyFlywayV5Applied() {
        Integer v5Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = 1",
                Integer.class
        );
        assertThat(v5Count).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-05 y HU-06 CA-1: permite seleccionar libremente cualquier unidad elegible aunque no sea la primera recomendada")
    void shouldAllowSelectingNonFirstEligibleAmbulanceAndPersistTraceability() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "BASIC", "N04"));

        Emergency emergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Incidente en N01")
        );

        List<EligibleAmbulanceCandidate> candidates =
                dispatchApplicationService.evaluateEligibleAmbulances(emergency.id().value()).candidates();
        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).ambulance().id().value()).isEqualTo("AMB-01");
        assertThat(candidates.get(1).ambulance().id().value()).isEqualTo("AMB-02");

        SupervisedDispatchResult result =
                dispatchApplicationService.assignSupervised(emergency.id().value(), "AMB-02");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(result.emergency().state()).isEqualTo(EmergencyState.ASSIGNED);
        assertThat(result.emergency().assignedAmbulanceId()).isEqualTo("AMB-02");

        Ambulance persistedAmb = ambulanceRepository.findById(AmbulanceId.of("AMB-02")).orElseThrow();
        assertThat(persistedAmb.state()).isEqualTo(AmbulanceState.ASSIGNED);
        assertThat(persistedAmb.assignedEmergencyId()).isEqualTo(emergency.id());

        assertThat(attemptLogRepository.findByEmergencyIdOrderByIdAsc(emergency.id().value())).hasSize(1);
        assertThat(operationResultRepository.findByEmergencyIdOrderByIdAsc(emergency.id().value())).hasSize(1);
    }

    @Test
    @DisplayName("HU-06 CA-2 y CA-3: concurrencia real sobre misma ambulancia impide doble asignación y transiciona a WAITING_AMBULANCE si no quedan elegibles")
    void shouldPreventDoubleAssignmentUnderConcurrentContentionInMySql() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));

        Emergency emg1 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Primera emergencia")
        );
        Emergency emg2 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N03", "Segunda emergencia")
        );

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Callable<SupervisedDispatchResult>> tasks = List.of(
                    () -> {
                        readyLatch.countDown();
                        startLatch.await(5, TimeUnit.SECONDS);
                        return dispatchApplicationService.assignSupervised(emg1.id().value(), "AMB-01");
                    },
                    () -> {
                        readyLatch.countDown();
                        startLatch.await(5, TimeUnit.SECONDS);
                        return dispatchApplicationService.assignSupervised(emg2.id().value(), "AMB-01");
                    }
            );

            List<Future<SupervisedDispatchResult>> futures = new ArrayList<>();
            for (Callable<SupervisedDispatchResult> task : tasks) {
                futures.add(pool.submit(task));
            }
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<SupervisedDispatchResult> outcomes = new ArrayList<>();
            for (Future<SupervisedDispatchResult> future : futures) {
                outcomes.add(future.get(10, TimeUnit.SECONDS));
            }

            long assignedCount = outcomes.stream()
                    .filter(r -> r.outcome() == DispatchOperationOutcome.ASSIGNED)
                    .count();
            long waitingCount = outcomes.stream()
                    .filter(r -> r.outcome() == DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE)
                    .count();

            assertThat(assignedCount).isEqualTo(1);
            assertThat(waitingCount).isEqualTo(1);

            Ambulance persistedAmb = ambulanceRepository.findById(AmbulanceId.of("AMB-01")).orElseThrow();
            assertThat(persistedAmb.state()).isEqualTo(AmbulanceState.ASSIGNED);

            SupervisedDispatchResult loser = outcomes.stream()
                    .filter(r -> r.outcome() == DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE)
                    .findFirst()
                    .orElseThrow();
            assertThat(loser.emergency().state()).isEqualTo(EmergencyState.WAITING_AMBULANCE);
            assertThat(attemptLogRepository.findByEmergencyIdOrderByIdAsc(loser.emergency().id().value()))
                    .anyMatch(log -> "AMBULANCE_UNAVAILABLE_CONFLICT".equals(log.getOutcomeCode()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("HU-07 MySQL: asignación automática concurrente evita doble asignación, reintenta y persiste trazabilidad")
    void shouldHandleConcurrentAutomaticDispatchWithoutDoubleAssignment() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "SPECIALIZED", "N02"));

        Emergency emg1 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-C", "N01", "Primera automática")
        );
        Emergency emg2 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Segunda automática")
        );
        Emergency emg3 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-D", "N10", "Nodo aislado N10")
        );

        AutomaticDispatchResult isolatedResult = dispatchApplicationService.assignAutomatic(emg3.id().value());
        assertThat(isolatedResult.outcome()).isEqualTo(DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE);
        assertThat(isolatedResult.attemptsExecuted()).isZero();
        assertThat(isolatedResult.emergency().state()).isEqualTo(EmergencyState.WAITING_AMBULANCE);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            List<Callable<AutomaticDispatchResult>> tasks = List.of(
                    () -> {
                        readyLatch.countDown();
                        startLatch.await(5, TimeUnit.SECONDS);
                        return dispatchApplicationService.assignAutomatic(emg1.id().value());
                    },
                    () -> {
                        readyLatch.countDown();
                        startLatch.await(5, TimeUnit.SECONDS);
                        return dispatchApplicationService.assignAutomatic(emg2.id().value());
                    }
            );
            List<Future<AutomaticDispatchResult>> futures = new ArrayList<>();
            for (Callable<AutomaticDispatchResult> task : tasks) {
                futures.add(pool.submit(task));
            }
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            AutomaticDispatchResult res1 = futures.get(0).get(10, TimeUnit.SECONDS);
            AutomaticDispatchResult res2 = futures.get(1).get(10, TimeUnit.SECONDS);

            assertThat(res1.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);
            assertThat(res2.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);
            assertThat(res1.assignedAmbulance().id()).isNotEqualTo(res2.assignedAmbulance().id());
            assertThat(operationResultRepository.findByEmergencyIdOrderByIdAsc(emg1.id().value()))
                    .anyMatch(r -> "ASSIGNED".equals(r.getFinalStatus()));
            assertThat(operationResultRepository.findByEmergencyIdOrderByIdAsc(emg2.id().value()))
                    .anyMatch(r -> "ASSIGNED".equals(r.getFinalStatus()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("HU-08 MySQL: cola WAITING_AMBULANCE respeta FIFO sin prioridad clínica, equipamiento y dispara por evento")
    void shouldProcessWaitingAmbulanceQueueInStrictFifoOrderViaSimulationTrigger() {
        logicalClock.advanceTo(LogicalTime.of(1L));
        Emergency lowFirst = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-D", "N02", "Baja primero")
        );
        dispatchApplicationService.moveToWaitingWhenNoEligible(lowFirst.id().value());

        logicalClock.advanceTo(LogicalTime.of(3L));
        Emergency highSecond = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Alta después")
        );
        dispatchApplicationService.moveToWaitingWhenNoEligible(highSecond.id().value());

        logicalClock.advanceTo(LogicalTime.of(5L));
        Emergency specThird = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-A", "N03", "Especializada después")
        );
        dispatchApplicationService.moveToWaitingWhenNoEligible(specThird.id().value());

        // Se registra una ambulancia BASIC: debe atender a lowFirst (FIFO t=1) y nunca a specThird (INC-A)
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        WaitingQueueEvaluationResult result = waitingQueueSimulationTrigger.handleEvent(
                new DiscreteEvent(
                        "EVT-Q1",
                        logicalClock.currentTime(),
                        DiscreteEventType.RESOURCE_RELEASE,
                        1L,
                        "AMB-01",
                        "AMB-01 pasa a AVAILABLE"
                )
        ).orElseThrow();

        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.remainingWaitingCount()).isEqualTo(2);
        assertThat(emergencyApplicationService.getEmergencyById(lowFirst.id().value()).state())
                .isEqualTo(EmergencyState.ASSIGNED);
        assertThat(emergencyApplicationService.getEmergencyById(highSecond.id().value()).state())
                .isEqualTo(EmergencyState.WAITING_AMBULANCE);
        assertThat(emergencyApplicationService.getEmergencyById(specThird.id().value()).state())
                .isEqualTo(EmergencyState.WAITING_AMBULANCE);
    }
}
