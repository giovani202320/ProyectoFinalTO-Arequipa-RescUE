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
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración contra MySQL 8.4 LTS que valida Flyway V6, llegada al incidente (HU-09),
 * reserva hospitalaria concurrente con SELECT ... FOR UPDATE (1+2) y cola FIFO WAITING_HOSPITAL (HU-10).
 */
@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Flyway V6 + Reserva Hospitalaria Concurrente y Cola WAITING_HOSPITAL (HU-09 y HU-10)")
class HospitalDispatchMySqlIntegrationTest {

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
    private EmergencyRepository emergencyRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private SpringDataHospitalReservationRepository springDataHospitalReservationRepository;

    @Autowired
    private SpringDataOperationAttemptLogRepository attemptLogRepository;

    @Autowired
    private SpringDataExperimentOperationResultRepository operationResultRepository;

    @Autowired
    private SpringDataAmbulanceRepository springDataAmbulanceRepository;

    @Autowired
    private SpringDataEmergencyRepository springDataEmergencyRepository;

    @Autowired
    private SpringDataHospitalRepository springDataHospitalRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabaseState() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM emergency_route_segments");
        springDataHospitalReservationRepository.deleteAll();
        attemptLogRepository.deleteAll();
        operationResultRepository.deleteAll();
        springDataAmbulanceRepository.deleteAll();
        springDataEmergencyRepository.deleteAll();
        springDataHospitalRepository.deleteAll();

        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("HOSPITAL", 1L));
    }

    @Test
    @DisplayName("Flyway V6 aplicada correctamente con tabla hospital_reservations")
    void shouldVerifyFlywayV6Applied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'hospital_reservations'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-09: ASSIGNED -> AT_INCIDENT -> IN_TRANSPORT/TRANSPORTING, incrementa R y rechaza llegada duplicada")
    void shouldConfirmArrivalReserveHospitalAndRejectDuplicateArrival() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Honorio Delgado", "N02", true, 2));
        Emergency registered = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Accidente de tránsito"));

        SupervisedDispatchResult dispatch = dispatchApplicationService.assignSupervised(registered.id().value(), "AMB-01");
        assertThat(dispatch.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);

        HospitalReservationResult arrival =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(registered.id().value());
        assertThat(arrival.outcome()).isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        assertThat(arrival.emergency().state()).isEqualTo(EmergencyState.IN_TRANSPORT);
        assertThat(arrival.emergency().reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(arrival.ambulance().state()).isEqualTo(AmbulanceState.TRANSPORTING);

        Hospital hospital = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
        assertThat(hospital.reservedSlots()).isEqualTo(1);
        assertThat(hospital.availableSlots()).isEqualTo(1);
        assertThat(springDataHospitalReservationRepository.count()).isEqualTo(1L);

        // Evento de llegada duplicado: idempotente, no duplica reserva ni incrementa R
        HospitalReservationResult duplicate =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(registered.id().value());
        assertThat(duplicate.outcome()).isEqualTo(HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE);
        assertThat(hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow().reservedSlots()).isEqualTo(1);
        assertThat(springDataHospitalReservationRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("HU-09 concurrencia real: dos llegadas compitiendo por D=1 preservan R+O<=C sin doble reserva")
    void shouldPreventOverbookingUnderConcurrentArrivalAndReservation() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Único", "N02", true, 1));

        Emergency emg1 = emergencyApplicationService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N01", "Caso 1"));
        Emergency emg2 = emergencyApplicationService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N01", "Caso 2"));

        assertThat(dispatchApplicationService.assignSupervised(emg1.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(dispatchApplicationService.assignSupervised(emg2.id().value(), "AMB-02").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<HospitalReservationResult> task1 = () -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                return hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg1.id().value());
            };
            Callable<HospitalReservationResult> task2 = () -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                return hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg2.id().value());
            };
            Future<HospitalReservationResult> f1 = pool.submit(task1);
            Future<HospitalReservationResult> f2 = pool.submit(task2);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<HospitalReservationResult> results = List.of(f1.get(15, TimeUnit.SECONDS), f2.get(15, TimeUnit.SECONDS));
            long inTransportCount = results.stream()
                    .filter(r -> r.outcome() == HospitalDispatchOutcome.IN_TRANSPORT)
                    .count();
            long waitingCount = results.stream()
                    .filter(r -> r.outcome() == HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL
                            || r.outcome() == HospitalDispatchOutcome.HOSPITAL_RESERVATION_RETRY_EXHAUSTED)
                    .count();

            assertThat(inTransportCount).isEqualTo(1L);
            assertThat(waitingCount).isEqualTo(1L);

            Hospital hospital = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
            assertThat(hospital.reservedSlots()).isEqualTo(1);
            assertThat(hospital.availableSlots()).isZero();
            assertThat(springDataHospitalReservationRepository.count()).isEqualTo(1L);

            HospitalReservationResult loser = results.stream()
                    .filter(r -> r.outcome() != HospitalDispatchOutcome.IN_TRANSPORT)
                    .findFirst()
                    .orElseThrow();
            Emergency loserDb = emergencyRepository.findById(loser.emergency().id()).orElseThrow();
            assertThat(loserDb.state()).isEqualTo(EmergencyState.WAITING_HOSPITAL);
            Ambulance loserAmb = ambulanceRepository.findById(AmbulanceId.of(loserDb.assignedAmbulanceId())).orElseThrow();
            assertThat(loserAmb.state()).isEqualTo(AmbulanceState.AT_INCIDENT);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("HU-10 CA-HOS-09: cola FIFO WAITING_HOSPITAL preserva posición de emergencia inalcanzable y atiende la siguiente")
    void shouldPreserveFifoPositionForUnreachableWaitingEmergencyAndServeNext() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N10"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Lleno", "N02", true, 0));

        // EMG-00001 en nodo aislado N10 (sin ruta hacia HOSP-01)
        Emergency unreachable = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N10", "Incidente aislado"));
        assertThat(dispatchApplicationService.assignSupervised(unreachable.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        HospitalReservationResult res1 =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(unreachable.id().value());
        assertThat(res1.outcome()).isEqualTo(HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL);

        // EMG-00002 en N01, inicialmente sin cupo en HOSP-01 (C=0)
        Emergency reachable = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Incidente en N01"));
        assertThat(dispatchApplicationService.assignSupervised(reachable.id().value(), "AMB-02").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        HospitalReservationResult res2 =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(reachable.id().value());
        assertThat(res2.outcome()).isEqualTo(HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL);

        Emergency unreachableBefore = emergencyRepository.findById(unreachable.id()).orElseThrow();

        // Se habilita capacidad en HOSP-01 (simulando liberación de cupo)
        jdbcTemplate.update("UPDATE hospitals SET capacity_total = 1 WHERE id = 'HOSP-01'");

        WaitingHospitalQueueEvaluationResult eval =
                hospitalDispatchApplicationService.evaluateWaitingHospitalQueue();

        assertThat(eval.evaluatedWaitingCount()).isEqualTo(2);
        assertThat(eval.reservedCount()).isEqualTo(1);
        assertThat(eval.remainingWaitingCount()).isEqualTo(1);

        // EMG-00001 sigue en WAITING_HOSPITAL conservando su marca original de cola (CA-HOS-09)
        Emergency unreachableAfter = emergencyRepository.findById(unreachable.id()).orElseThrow();
        assertThat(unreachableAfter.state()).isEqualTo(EmergencyState.WAITING_HOSPITAL);
        assertThat(unreachableAfter.hospitalQueueEnteredAt()).isEqualTo(unreachableBefore.hospitalQueueEnteredAt());
        assertThat(ambulanceRepository.findById(AmbulanceId.of("AMB-01")).orElseThrow().state())
                .isEqualTo(AmbulanceState.AT_INCIDENT);

        // EMG-00002 pasa a IN_TRANSPORT y AMB-02 a TRANSPORTING
        Emergency reachableAfter = emergencyRepository.findById(reachable.id()).orElseThrow();
        assertThat(reachableAfter.state()).isEqualTo(EmergencyState.IN_TRANSPORT);
        assertThat(reachableAfter.reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(ambulanceRepository.findById(AmbulanceId.of("AMB-02")).orElseThrow().state())
                .isEqualTo(AmbulanceState.TRANSPORTING);
    }
}
