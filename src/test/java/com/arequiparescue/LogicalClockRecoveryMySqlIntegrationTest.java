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
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeAttemptExecutor;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.shared.infrastructure.LogicalClockStartupSynchronizer;
import com.arequiparescue.simulation.application.ScheduleSimulationEventCommand;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.application.SimulationStepOutcome;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.DiscreteEventType;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("P0-1 MySQL: Hidratación de LogicalClock tras reinicio, entrega/alta histórica y múltiples escenarios")
class LogicalClockRecoveryMySqlIntegrationTest {

    @Autowired
    private EmergencyApplicationService emergencyApplicationService;

    @Autowired
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @Autowired
    private HospitalCatalogApplicationService hospitalCatalogService;

    @Autowired
    private DispatchApplicationService dispatchApplicationService;

    @Autowired
    private HospitalDispatchApplicationService hospitalDispatchApplicationService;

    @Autowired
    private HospitalDeliveryAndDischargeAttemptExecutor deliveryAndDischargeAttemptExecutor;

    @Autowired
    private HospitalReservationRepository hospitalReservationRepository;

    @Autowired
    private SimulationScenarioApplicationService simulationScenarioService;

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
    private SpringDataAmbulanceRepository ambulanceJpaRepository;

    @Autowired
    private SpringDataEmergencyRepository emergencyJpaRepository;

    @Autowired
    private SpringDataHospitalRepository hospitalJpaRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository sequenceJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabaseState() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM emergency_route_segments");
        simulationEventJpaRepository.deleteAll();
        simulationScenarioJpaRepository.deleteAll();
        hospitalReservationJpaRepository.deleteAll();
        attemptLogJpaRepository.deleteAll();
        operationResultJpaRepository.deleteAll();
        ambulanceJpaRepository.deleteAll();
        emergencyJpaRepository.deleteAll();
        hospitalJpaRepository.deleteAll();
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
        sequenceJpaRepository.saveAndFlush(new EmergencySequenceJpaEntity("HOSPITAL", 1L));
    }

    @Test
    @DisplayName("Caso 1: arranque sin datos históricos mantiene el reloj lógico en t=0")
    void shouldKeepClockAtZeroWhenDatabaseHasNoHistoricalTimes() {
        LogicalClock freshClock = new LogicalClock(LogicalTime.ZERO);

        LogicalTime hydrated = LogicalClockStartupSynchronizer.synchronizeFromDatabase(freshClock, jdbcTemplate);

        assertThat(hydrated).isEqualTo(LogicalTime.ZERO);
        assertThat(freshClock.currentTicks()).isZero();
    }

    @Test
    @DisplayName("Casos 2 y 3: reinicio con reserva reserved_at_t=4 y occupied_at_t=5 completa entrega y alta sin error")
    void shouldRecoverDeliveryAndDischargeAfterRestartWithHistoricalTimestamps() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Base", "N04", true, 2));
        Emergency emergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Caso histórico"));

        assertThat(dispatchApplicationService.assignSupervised(emergency.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emergency.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);

        HospitalReservation reserved = hospitalReservationRepository.findAllOrderByReservedAtAndId().stream()
                .filter(r -> r.emergencyId().equals(emergency.id()))
                .findFirst()
                .orElseThrow();
        // Simula registro histórico persistido en t=4 (como RES-EMG-00024 en arequipa_rescue),
        // normalizando timestamps de la emergencia para aislar el orden de ejecución del contexto Spring
        jdbcTemplate.update(
                "UPDATE emergencies SET registered_at_t = 0, assigned_at_t = 1, at_incident_at_t = 2, in_transport_at_t = 4, completed_at_t = NULL WHERE id = ?",
                emergency.id().value());
        jdbcTemplate.update("UPDATE hospital_reservations SET reserved_at_t = 4 WHERE id = ?", reserved.id());

        // Simula reinicio con reloj nuevo en t=0: hidratación al arranque recupera t=4
        LogicalClock restartedClockForDelivery = new LogicalClock(LogicalTime.ZERO);
        LogicalClockStartupSynchronizer.synchronizeFromDatabase(restartedClockForDelivery, jdbcTemplate);
        assertThat(restartedClockForDelivery.currentTicks()).isEqualTo(4L);

        // Incluso invocando entrega con t=0 directo, el piso monotónico evita IllegalArgumentException
        HospitalDeliveryAndDischargeAttemptExecutor.DeliveryExecutionOutcome deliveryOutcome =
                deliveryAndDischargeAttemptExecutor.executeHospitalDelivery(emergency.id(), LogicalTime.ZERO);
        assertThat(deliveryOutcome.completed()).isTrue();
        assertThat(deliveryOutcome.emergency().state()).isEqualTo(EmergencyState.COMPLETED);
        assertThat(deliveryOutcome.reservation().status()).isEqualTo(HospitalReservationStatus.OCCUPIED);
        assertThat(deliveryOutcome.reservation().reservedAt().ticks()).isEqualTo(4L);
        assertThat(deliveryOutcome.reservation().occupiedAt().ticks()).isEqualTo(4L);

        // Simula registro histórico ocupado en t=5 (como RES-EMG-00017 en arequipa_rescue)
        jdbcTemplate.update("UPDATE hospital_reservations SET occupied_at_t = 5 WHERE id = ?", reserved.id());

        LogicalClock restartedClockForDischarge = new LogicalClock(LogicalTime.ZERO);
        LogicalClockStartupSynchronizer.synchronizeFromDatabase(restartedClockForDischarge, jdbcTemplate);
        assertThat(restartedClockForDischarge.currentTicks()).isEqualTo(5L);

        // Alta simulada tras reinicio funciona tanto con reloj hidratado como con piso monotónico en t=0
        HospitalDeliveryAndDischargeAttemptExecutor.DischargeExecutionOutcome dischargeOutcome =
                deliveryAndDischargeAttemptExecutor.executeSimulatedDischarge(
                        reserved.id(), HospitalId.of("HOSP-01"), LogicalTime.ZERO);
        assertThat(dischargeOutcome.discharged()).isTrue();
        assertThat(dischargeOutcome.reservation().status()).isEqualTo(HospitalReservationStatus.DISCHARGED);
        assertThat(dischargeOutcome.reservation().occupiedAt().ticks()).isEqualTo(5L);
        assertThat(dischargeOutcome.reservation().dischargedAt().ticks()).isEqualTo(5L);
        assertThat(dischargeOutcome.hospital().occupiedSlots()).isZero();
        assertThat(dischargeOutcome.hospital().availableSlots()).isEqualTo(2);
    }

    @Test
    @DisplayName("Caso 6: compatibilidad con múltiples escenarios tras hidratar LogicalClock desde escenario previo")
    void shouldRemainCompatibleWithMultipleIndependentScenariosAfterHydration() {
        // Escenario 1 avanza hasta t=6 y finaliza
        SimulationScenario sc1 = simulationScenarioService.createScenario("SCN-REC-1", "Escenario previo");
        simulationScenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                sc1.id(), "EVT-REC-1", 6L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-HIST", "Hito t=6"));
        SimulationStepResult step1 = simulationScenarioService.advanceNextStep(sc1.id());
        assertThat(step1.outcome()).isEqualTo(SimulationStepOutcome.EXECUTED);
        simulationScenarioService.completeScenario(sc1.id());

        // Simula reinicio: el reloj global se hidrata a t=6
        LogicalClock restartedClock = new LogicalClock(LogicalTime.ZERO);
        LogicalClockStartupSynchronizer.synchronizeFromDatabase(restartedClock, jdbcTemplate);
        assertThat(restartedClock.currentTicks()).isEqualTo(6L);

        // Escenario 2 nace en t=0 y puede programar/ejecutar eventos en t=2 sin conflicto ni retroceso del reloj global
        SimulationScenario sc2 = simulationScenarioService.createScenario("SCN-REC-2", "Segundo escenario");
        assertThat(sc2.currentTime()).isEqualTo(LogicalTime.ZERO);
        simulationScenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                sc2.id(), "EVT-REC-2", 2L, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-NEW", "Hito t=2"));
        SimulationStepResult step2 = simulationScenarioService.advanceNextStep(sc2.id());
        assertThat(step2.outcome()).isEqualTo(SimulationStepOutcome.EXECUTED);
        assertThat(step2.currentTime()).isEqualTo(LogicalTime.of(2L));

        // Sincronizar con t=2 no retrocede el reloj global desde t=6; avanzar a t=8 sí lo adelanta
        assertThat(restartedClock.synchronizeAtLeast(step2.currentTime()).ticks()).isEqualTo(6L);
        assertThat(restartedClock.synchronizeAtLeast(LogicalTime.of(8L)).ticks()).isEqualTo(8L);
    }
}
