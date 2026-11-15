package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.ambulance.infrastructure.persistence.SpringDataAmbulanceRepository;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.SupervisedDispatchResult;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataExperimentOperationResultRepository;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataOperationAttemptLogRepository;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.application.ScheduleSimulationEventCommand;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.application.SimulationStepOutcome;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.DiscreteEventStatus;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.ScenarioStatus;
import com.arequiparescue.simulation.domain.SimulationScenario;
import com.arequiparescue.simulation.infrastructure.persistence.SpringDataSimulationEventRepository;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
class SimulationScenarioMySqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SimulationScenarioApplicationService scenarioService;

    @Autowired
    private EmergencyApplicationService emergencyService;

    @Autowired
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @Autowired
    private HospitalCatalogApplicationService hospitalCatalogService;

    @Autowired
    private DispatchApplicationService dispatchService;

    @Autowired
    private HospitalDispatchApplicationService hospitalDispatchService;

    @Autowired
    private HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeService;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private SpringDataSimulationEventRepository simulationEventJpaRepository;

    @Autowired
    private SpringDataSimulationScenarioRepository simulationScenarioJpaRepository;

    @Autowired
    private SpringDataHospitalReservationRepository hospitalReservationJpaRepository;

    @Autowired
    private SpringDataOperationAttemptLogRepository attemptLogJpaRepository;

    @Autowired
    private SpringDataExperimentOperationResultRepository operationResultJpaRepository;

    @Autowired
    private SpringDataEmergencyRepository emergencyJpaRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository sequenceJpaRepository;

    @Autowired
    private SpringDataAmbulanceRepository ambulanceJpaRepository;

    @Autowired
    private SpringDataHospitalRepository hospitalJpaRepository;

    @BeforeEach
    void setUp() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        cleanDatabase();
    }

    private void cleanDatabase() {
        simulationEventJpaRepository.deleteAll();
        simulationScenarioJpaRepository.deleteAll();
        hospitalReservationJpaRepository.deleteAll();
        attemptLogJpaRepository.deleteAll();
        operationResultJpaRepository.deleteAll();
        emergencyJpaRepository.deleteAll();
        ambulanceJpaRepository.deleteAll();
        hospitalJpaRepository.deleteAll();
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("HOSPITAL", 1L));
    }

    @Test
    @DisplayName("HU-13: avance manual paso a paso y desempate determinista persistido en MySQL")
    void advancesClockDeterministicallyAndHandlesNoPendingEvents() {
        SimulationScenario created = scenarioService.createScenario("SCN-DET", "Determinismo HU-13");
        assertEquals(ScenarioStatus.ACTIVE, created.status());
        assertEquals(LogicalTime.ZERO, created.currentTime());

        // Dos eventos en t=5 con distinta precedencia y otro en t=10
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-DET", "EV-REG", 5L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-0001", "Registro"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-DET", "EV-REL", 5L, DiscreteEventType.RESOURCE_RELEASE, "SYS-REL", "Liberación"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-DET", "EV-ARR", 5L, DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, "EMG-9999", "Llegada"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-DET", "EV-Q10", 10L, DiscreteEventType.WAITING_QUEUE_EVALUATION, "SYS-Q", "Cola t=10"));

        // Paso 1: debe elegir RESOURCE_RELEASE en t=5 (rank 1)
        SimulationStepResult step1 = scenarioService.advanceNextStep("SCN-DET");
        assertEquals(SimulationStepOutcome.EXECUTED, step1.outcome());
        assertEquals("EV-REL", step1.processedEvent().event().eventId());
        assertEquals(LogicalTime.of(5), step1.currentTime());

        // Paso 2: debe elegir AMBULANCE_ARRIVAL_AT_INCIDENT en t=5 (rank 2) - falla porque EMG-9999 no existe, queda FAILED trazable
        SimulationStepResult step2 = scenarioService.advanceNextStep("SCN-DET");
        assertEquals(SimulationStepOutcome.FAILED, step2.outcome());
        assertEquals("EV-ARR", step2.processedEvent().event().eventId());
        assertEquals(DiscreteEventStatus.FAILED, step2.processedEvent().status());
        assertEquals(LogicalTime.of(5), step2.currentTime());

        // Paso 3: debe elegir EMERGENCY_REGISTRATION en t=5 (rank 4)
        SimulationStepResult step3 = scenarioService.advanceNextStep("SCN-DET");
        assertEquals(SimulationStepOutcome.EXECUTED, step3.outcome());
        assertEquals("EV-REG", step3.processedEvent().event().eventId());
        assertEquals(LogicalTime.of(5), step3.currentTime());

        // Paso 4: debe elegir WAITING_QUEUE_EVALUATION en t=10
        SimulationStepResult step4 = scenarioService.advanceNextStep("SCN-DET");
        assertEquals(SimulationStepOutcome.EXECUTED, step4.outcome());
        assertEquals("EV-Q10", step4.processedEvent().event().eventId());
        assertEquals(LogicalTime.of(10), step4.currentTime());

        // Paso 5: sin eventos pendientes -> no altera el reloj
        SimulationStepResult step5 = scenarioService.advanceNextStep("SCN-DET");
        assertEquals(SimulationStepOutcome.NO_PENDING_EVENTS, step5.outcome());
        assertEquals(LogicalTime.of(10), step5.currentTime());

        // Persistencia verificada tras lectura
        SimulationScenario reloaded = scenarioService.findScenario("SCN-DET").orElseThrow();
        assertEquals(LogicalTime.of(10), reloaded.currentTime());

        // No se permite programar en el pasado (t=9 < t=10) ni duplicar ID de evento
        assertThrows(IllegalArgumentException.class, () -> scenarioService.scheduleEvent(
                new ScheduleSimulationEventCommand(
                        "SCN-DET", "EV-PAST", 9L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-1", "Pasado")));
        assertThrows(IllegalArgumentException.class, () -> scenarioService.scheduleEvent(
                new ScheduleSimulationEventCommand(
                        "SCN-DET", "EV-REG", 12L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-1", "Duplicado")));

        // Cierre del escenario
        SimulationScenario completed = scenarioService.completeScenario("SCN-DET");
        assertEquals(ScenarioStatus.COMPLETED, completed.status());
        assertThrows(IllegalStateException.class, () -> scenarioService.advanceNextStep("SCN-DET"));
    }

    @Test
    @DisplayName("HU-13: dos avances concurrentes sobre el mismo escenario no duplican evento ni retroceden el reloj")
    void concurrentAdvancesDoNotDuplicateEventOrRegressClock() throws Exception {
        scenarioService.createScenario("SCN-CONC", "Concurrencia HU-13");
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-CONC", "EV-01", 4L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-1", "Paso 1"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-CONC", "EV-02", 8L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-2", "Paso 2"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<SimulationStepResult>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return scenarioService.advanceNextStep("SCN-CONC");
                }));
            }
            ready.await();
            start.countDown();

            List<String> executedIds = new ArrayList<>();
            for (Future<SimulationStepResult> future : futures) {
                SimulationStepResult result = future.get();
                assertEquals(SimulationStepOutcome.EXECUTED, result.outcome());
                assertNotNull(result.processedEvent());
                executedIds.add(result.processedEvent().event().eventId());
            }

            assertTrue(executedIds.contains("EV-01"));
            assertTrue(executedIds.contains("EV-02"));
            assertEquals(2, executedIds.stream().distinct().count());
            assertEquals(LogicalTime.of(8), scenarioService.findScenario("SCN-CONC").orElseThrow().currentTime());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("HU-13: integración con flujo real de emergencia, despacho, llegada al incidente y entrega hospitalaria")
    void integratesManualStepAdvanceWithEmergencyDispatchAndHospitalFlow() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Base", "N03", true, 2));

        // Iniciar escenario primero (RN-HOS-015 bloquea crear hospitales si hay escenario ACTIVE, por eso se inicia después del setup de catálogo)
        scenarioService.createScenario("SCN-FLOW", "Flujo completo HU-13");

        Emergency emergency = emergencyService.registerEmergency(new RegisterEmergencyCommand(
                "INC-B", "N02", "Paciente grave en N02"));
        SupervisedDispatchResult dispatch = dispatchService.assignSupervised(emergency.id().value(), "AMB-01");
        assertEquals(DispatchOperationOutcome.ASSIGNED, dispatch.outcome());

        // Agendar llegada al incidente en t=3 y entrega hospitalaria en t=7
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-FLOW",
                "EV-ARR-1",
                3L,
                DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT,
                emergency.id().value(),
                "Llegada al incidente"
        ));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-FLOW",
                "EV-DEL-1",
                7L,
                DiscreteEventType.RESOURCE_RELEASE,
                emergency.id().value(),
                "Entrega hospitalaria"
        ));

        SimulationStepResult arrivalStep = scenarioService.advanceNextStep("SCN-FLOW");
        assertEquals(SimulationStepOutcome.EXECUTED, arrivalStep.outcome());
        assertEquals(LogicalTime.of(3), scenarioService.findScenario("SCN-FLOW").orElseThrow().currentTime());
        Emergency afterArrival = emergencyRepository.findById(emergency.id()).orElseThrow();
        assertEquals(EmergencyState.IN_TRANSPORT, afterArrival.state());

        SimulationStepResult deliveryStep = scenarioService.advanceNextStep("SCN-FLOW");
        assertEquals(SimulationStepOutcome.EXECUTED, deliveryStep.outcome());
        assertEquals(LogicalTime.of(7), scenarioService.findScenario("SCN-FLOW").orElseThrow().currentTime());
        Emergency afterDelivery = emergencyRepository.findById(emergency.id()).orElseThrow();
        Ambulance afterAmbulance = ambulanceRepository.findById(AmbulanceId.of("AMB-01")).orElseThrow();
        Hospital afterHospital = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
        assertEquals(EmergencyState.COMPLETED, afterDelivery.state());
        assertEquals(AmbulanceState.AVAILABLE, afterAmbulance.state());
        assertEquals(1, afterHospital.occupiedSlots());
    }

    @Test
    @DisplayName("HU-13: escenario COMPLETED conserva eventos PENDING para trazabilidad pero impide avanzar o programar")
    void completedScenarioPreservesPendingEventsForTraceabilityAndRejectsAdvanceOrSchedule() {
        scenarioService.createScenario("SCN-02", "Cierre anticipado");
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-02", "EV2-01", 6L, DiscreteEventType.WAITING_QUEUE_EVALUATION, "SYS-Q", "Evaluación"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-02", "EV2-02", 7L, DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, "AMB-02", "Pendiente al cierre"));

        SimulationStepResult step1 = scenarioService.advanceNextStep("SCN-02");
        assertEquals(SimulationStepOutcome.EXECUTED, step1.outcome());
        assertEquals(LogicalTime.of(6), step1.currentTime());

        SimulationScenario completed = scenarioService.completeScenario("SCN-02");
        assertEquals(ScenarioStatus.COMPLETED, completed.status());
        assertEquals(LogicalTime.of(6), completed.currentTime());

        // El evento pendiente permanece en MySQL para trazabilidad sin cancelar ni alterar su estado
        List<ScheduledSimulationEvent> pending = scenarioService.listPendingEvents("SCN-02");
        assertEquals(1, pending.size());
        assertEquals("EV2-02", pending.getFirst().event().eventId());
        assertEquals(DiscreteEventStatus.PENDING, pending.getFirst().status());

        // Un escenario COMPLETED no puede avanzar ni aceptar nuevos eventos
        assertThrows(IllegalStateException.class, () -> scenarioService.advanceNextStep("SCN-02"));
        assertThrows(IllegalStateException.class, () -> scenarioService.scheduleEvent(
                new ScheduleSimulationEventCommand(
                        "SCN-02", "EV2-03", 8L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-0001", "Post cierre")));
    }

    @Test
    @DisplayName("HU-13: distingue ejecución válida EXECUTED de ejecución fallida FAILED con sujeto inválido HOSP")
    void distinguishesValidExecutedEventFromInvalidFailedEventWithTraceability() {
        scenarioService.createScenario("SCN-VAL", "Validación EXECUTED vs FAILED");
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-VAL", "EV-VALID", 2L, DiscreteEventType.WAITING_QUEUE_EVALUATION, "SYS-Q", "Válido"));
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-VAL", "EV-BAD", 3L, DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, "HOSP", "Sujeto inválido"));

        SimulationStepResult validResult = scenarioService.advanceNextStep("SCN-VAL");
        assertEquals(SimulationStepOutcome.EXECUTED, validResult.outcome());
        assertEquals(DiscreteEventStatus.EXECUTED, validResult.processedEvent().status());
        assertTrue(validResult.processedEvent().executionNote().contains("EV-VALID"));

        SimulationStepResult failedResult = scenarioService.advanceNextStep("SCN-VAL");
        assertEquals(SimulationStepOutcome.FAILED, failedResult.outcome());
        assertEquals(DiscreteEventStatus.FAILED, failedResult.processedEvent().status());
        assertTrue(failedResult.processedEvent().executionNote().contains("EMG-XXXXX"));
        assertTrue(failedResult.processedEvent().executionNote().contains("HOSP"));

        List<ScheduledSimulationEvent> history = scenarioService.listScenarioEvents("SCN-VAL");
        assertEquals(2, history.size());
        assertEquals(DiscreteEventStatus.EXECUTED, history.get(0).status());
        assertEquals(DiscreteEventStatus.FAILED, history.get(1).status());
    }
}
