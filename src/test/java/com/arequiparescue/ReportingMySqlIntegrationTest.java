package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.infrastructure.persistence.SpringDataAmbulanceRepository;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataExperimentOperationResultRepository;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataOperationAttemptLogRepository;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
import com.arequiparescue.reporting.application.ReportingApplicationService;
import com.arequiparescue.reporting.domain.SimulationReportSnapshot;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.simulation.application.ScheduleSimulationEventCommand;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.domain.DiscreteEventType;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
class ReportingMySqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportingApplicationService reportingService;

    @Autowired
    private LogicalClock logicalClock;

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
    @AfterEach
    void resetDatabase() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
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
    @DisplayName("HU-14: calcula las 7 métricas y registro trazable desde MySQL sin alterar datos operacionales")
    void computesAllSevenMetricsAndTraceabilityFromMySql() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Central", "N03", true, 1));
        scenarioService.createScenario("SCN-REP", "Reporte HU-14");

        long baseT = logicalClock.currentTicks();

        // Avanzar reloj a baseT + 2 antes de registrar EMG-00001
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-REP", "EV-T2", baseT + 2L, DiscreteEventType.EMERGENCY_REGISTRATION, "SYS", "Tick +2"));
        scenarioService.advanceNextStep("SCN-REP");

        Emergency emg1 = emergencyService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N02", "Caso 1"));
        assertEquals(DispatchOperationOutcome.ASSIGNED, dispatchService.assignSupervised(emg1.id().value(), "AMB-01").outcome());

        // EMG-00002 SPECIALIZED sin ambulancia especializada disponible -> permanece en WAITING_AMBULANCE (EN_ESPERA)
        Emergency emg2 = emergencyService.registerEmergency(new RegisterEmergencyCommand("INC-A", "N02", "Caso 2"));
        assertEquals(DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE, dispatchService.assignAutomatic(emg2.id().value()).outcome());

        // Avanzar a baseT + 6 para llegada al incidente de EMG-00001
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-REP", "EV-ARR", baseT + 6L, DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, emg1.id().value(), "Llegada"));
        scenarioService.advanceNextStep("SCN-REP");

        // Avanzar a baseT + 10 para entrega hospitalaria de EMG-00001
        scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                "SCN-REP", "EV-DEL", baseT + 10L, DiscreteEventType.RESOURCE_RELEASE, emg1.id().value(), "Entrega"));
        scenarioService.advanceNextStep("SCN-REP");

        SimulationReportSnapshot report = reportingService.generateReport("SCN-REP", null).snapshot();

        assertEquals("SCN-REP", report.referenceScenarioId());
        assertEquals(baseT + 10L, report.currentLogicalTicks());
        assertEquals(1L, report.completedEmergencies().completedCount());
        assertEquals(1L, report.completedEmergencies().activeCount());
        assertEquals(1L, report.waitingForAmbulance().closedCount());
        assertEquals(0.0, report.waitingForAmbulance().closedAverageTicks());
        assertEquals(1L, report.waitingForAmbulance().inProgressCount());
        assertEquals(1L, report.responseToIncident().closedCount());
        assertEquals(4.0, report.responseToIncident().closedAverageTicks());
        assertEquals(1L, report.pickupToHospital().closedCount());
        assertEquals(4.0, report.pickupToHospital().closedAverageTicks());
        assertEquals(1L, report.waitingForHospital().closedCount());
        assertEquals(0.0, report.waitingForHospital().closedAverageTicks());
        assertTrue(report.ambulanceUtilization().totalOccupiedTicks() >= 8L);
        assertEquals(2, report.experimentalRecords().size());
    }

    @Test
    @DisplayName("HU-14: lecturas concurrentes de reportes no bloquean ni alteran operaciones transaccionales")
    void concurrentReportReadsDoNotAlterOperationalData() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Central", "N03", true, 2));
        Emergency emg = emergencyService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N02", "Concurrente"));

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                assertEquals(DispatchOperationOutcome.ASSIGNED, dispatchService.assignAutomatic(emg.id().value()).outcome());
                hospitalDispatchService.confirmArrivalAndReserveHospital(emg.id().value());
                deliveryAndDischargeService.confirmHospitalDelivery(emg.id().value());
                return null;
            }));
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    SimulationReportSnapshot snapshot = reportingService.generateReport(null, null).snapshot();
                    assertNotNull(snapshot);
                    return snapshot;
                }));
            }
            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get();
            }

            SimulationReportSnapshot after = reportingService.generateReport(null, null).snapshot();
            assertEquals(1L, after.completedEmergencies().completedCount());
        } finally {
            pool.shutdownNow();
        }
    }
}
