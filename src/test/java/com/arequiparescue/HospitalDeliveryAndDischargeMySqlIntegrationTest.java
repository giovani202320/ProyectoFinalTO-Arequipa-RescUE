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
import com.arequiparescue.hospital.application.HospitalDeliveryResult;
import com.arequiparescue.hospital.application.HospitalDischargeResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
import com.arequiparescue.routing.domain.NodeId;
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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración contra MySQL 8.4 LTS que valida el ciclo completo de entrega hospitalaria
 * (HU-11: IN_TRANSPORT -> COMPLETED, RESERVED -> OCCUPIED, R-1, O+1, TRANSPORTING -> AVAILABLE +
 * reevaluación de WAITING_AMBULANCE) y alta hospitalaria simulada (HU-12: OCCUPIED -> DISCHARGED,
 * O-1, D+1, idempotencia concurrente y reevaluación FIFO de WAITING_HOSPITAL).
 */
@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Entrega Hospitalaria HU-11 y Alta Simulada Idempotente HU-12")
class HospitalDeliveryAndDischargeMySqlIntegrationTest {

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
    private HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeApplicationService;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalReservationRepository hospitalReservationRepository;

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
    @DisplayName("HU-11 y HU-12: ciclo completo ASSIGNED -> IN_TRANSPORT -> COMPLETED, RESERVED -> OCCUPIED -> DISCHARGED y entrega duplicada idempotente")
    void shouldCompleteFullHospitalCycleAndRejectDuplicateDelivery() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Central", "N04", true, 1));
        Emergency emergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Trauma moderado"));

        assertThat(dispatchApplicationService.assignSupervised(emergency.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emergency.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);

        // HU-11: entrega hospitalaria atómica
        HospitalDeliveryResult delivery =
                deliveryAndDischargeApplicationService.confirmHospitalDelivery(emergency.id().value());
        assertThat(delivery.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
        assertThat(delivery.emergency().state()).isEqualTo(EmergencyState.COMPLETED);
        assertThat(delivery.emergency().assignedAmbulanceId()).isEqualTo("AMB-01");
        assertThat(delivery.emergency().reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(delivery.releasedAmbulance().state()).isEqualTo(AmbulanceState.AVAILABLE);
        assertThat(delivery.releasedAmbulance().currentNode()).isEqualTo(NodeId.of("N04"));
        assertThat(delivery.releasedAmbulance().assignedEmergencyId()).isNull();
        assertThat(delivery.hospital().reservedSlots()).isZero();
        assertThat(delivery.hospital().occupiedSlots()).isEqualTo(1);
        assertThat(delivery.hospital().availableSlots()).isZero();
        assertThat(delivery.reservation().status()).isEqualTo(HospitalReservationStatus.OCCUPIED);

        // Entrega duplicada no altera contadores ni estados
        HospitalDeliveryResult duplicateDelivery =
                deliveryAndDischargeApplicationService.confirmHospitalDelivery(emergency.id().value());
        assertThat(duplicateDelivery.outcome()).isEqualTo(HospitalDeliveryOutcome.DUPLICATE_OR_INVALID_STATE);
        Hospital hospAfterDup = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
        assertThat(hospAfterDup.reservedSlots()).isZero();
        assertThat(hospAfterDup.occupiedSlots()).isEqualTo(1);

        // HU-12: alta hospitalaria simulada
        HospitalDischargeResult discharge =
                deliveryAndDischargeApplicationService.executeSimulatedDischarge(delivery.reservation().id());
        assertThat(discharge.outcome()).isEqualTo(HospitalDischargeOutcome.DISCHARGED);
        assertThat(discharge.reservation().status()).isEqualTo(HospitalReservationStatus.DISCHARGED);
        assertThat(discharge.hospital().occupiedSlots()).isZero();
        assertThat(discharge.hospital().availableSlots()).isEqualTo(1);

        // Emergencia sigue COMPLETED y ambulancia sigue AVAILABLE
        assertThat(emergencyRepository.findById(emergency.id()).orElseThrow().state())
                .isEqualTo(EmergencyState.COMPLETED);
        assertThat(ambulanceRepository.findById(AmbulanceId.of("AMB-01")).orElseThrow().state())
                .isEqualTo(AmbulanceState.AVAILABLE);
    }

    @Test
    @DisplayName("HU-11: entrega hospitalaria libera ambulancia y asigna automáticamente emergencia en WAITING_AMBULANCE")
    void shouldAssignWaitingAmbulanceEmergencyWhenHospitalDeliveryFreesAmbulance() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Central", "N04", true, 2));

        Emergency first = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Primera emergencia"));
        assertThat(dispatchApplicationService.assignSupervised(first.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(first.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);

        // Segunda emergencia queda en WAITING_AMBULANCE porque no hay ambulancias AVAILABLE
        Emergency waiting = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Emergencia en espera"));
        SupervisedDispatchResult waitRes =
                dispatchApplicationService.assignSupervised(waiting.id().value(), "AMB-01");
        assertThat(waitRes.outcome()).isEqualTo(DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE);

        // Entrega de la primera libera AMB-01 y dispara reevaluación de WAITING_AMBULANCE
        HospitalDeliveryResult delivery =
                deliveryAndDischargeApplicationService.confirmHospitalDelivery(first.id().value());
        assertThat(delivery.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
        assertThat(delivery.ambulanceQueueEvaluation().assignedCount()).isEqualTo(1);

        Emergency waitingAfter = emergencyRepository.findById(waiting.id()).orElseThrow();
        assertThat(waitingAfter.state()).isEqualTo(EmergencyState.ASSIGNED);
        assertThat(waitingAfter.assignedAmbulanceId()).isEqualTo("AMB-01");

        Ambulance ambAfter = ambulanceRepository.findById(AmbulanceId.of("AMB-01")).orElseThrow();
        assertThat(ambAfter.state()).isEqualTo(AmbulanceState.ASSIGNED);
        assertThat(ambAfter.assignedEmergencyId()).isEqualTo(waiting.id());
    }

    @Test
    @DisplayName("HU-12 concurrencia e integración FIFO: dos altas concurrentes sobre la misma ocupación no duplican liberación y disparan reserva en WAITING_HOSPITAL")
    void shouldEnsureIdempotentConcurrentDischargeAndTriggerWaitingHospitalFifoReservation() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Único", "N02", true, 1));

        // EMG-1 ocupa la única plaza (C=1 -> O=1, D=0)
        Emergency emg1 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Caso 1"));
        assertThat(dispatchApplicationService.assignSupervised(emg1.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        HospitalReservationResult res1 =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg1.id().value());
        assertThat(res1.outcome()).isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        HospitalDeliveryResult del1 =
                deliveryAndDischargeApplicationService.confirmHospitalDelivery(emg1.id().value());
        assertThat(del1.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
        String occupiedReservationId = del1.reservation().id();

        // EMG-2 llega al incidente mientras D=0 -> WAITING_HOSPITAL reteniendo AMB-02 en AT_INCIDENT
        Emergency emg2 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N01", "Caso 2 en espera hospitalaria"));
        assertThat(dispatchApplicationService.assignSupervised(emg2.id().value(), "AMB-02").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        HospitalReservationResult res2 =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg2.id().value());
        assertThat(res2.outcome()).isEqualTo(HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL);

        // Concurrencia: dos hilos ejecutan alta simulada sobre occupiedReservationId simultáneamente
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<HospitalDischargeResult> task = () -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                return deliveryAndDischargeApplicationService.executeSimulatedDischarge(occupiedReservationId);
            };
            Future<HospitalDischargeResult> f1 = pool.submit(task);
            Future<HospitalDischargeResult> f2 = pool.submit(task);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<HospitalDischargeResult> outcomes = List.of(f1.get(15, TimeUnit.SECONDS), f2.get(15, TimeUnit.SECONDS));
            long dischargedCount = outcomes.stream()
                    .filter(r -> r.outcome() == HospitalDischargeOutcome.DISCHARGED)
                    .count();
            long duplicateCount = outcomes.stream()
                    .filter(r -> r.outcome() == HospitalDischargeOutcome.DUPLICATE_OR_INVALID_STATE)
                    .count();

            assertThat(dischargedCount).isEqualTo(1L);
            assertThat(duplicateCount).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }

        // La plaza liberada debe haber quedado reservada automáticamente por EMG-2 (WAITING_HOSPITAL -> IN_TRANSPORT)
        HospitalReservation oldRes = hospitalReservationRepository.findById(occupiedReservationId).orElseThrow();
        assertThat(oldRes.status()).isEqualTo(HospitalReservationStatus.DISCHARGED);

        Emergency emg2After = emergencyRepository.findById(emg2.id()).orElseThrow();
        assertThat(emg2After.state()).isEqualTo(EmergencyState.IN_TRANSPORT);
        assertThat(emg2After.reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(ambulanceRepository.findById(AmbulanceId.of("AMB-02")).orElseThrow().state())
                .isEqualTo(AmbulanceState.TRANSPORTING);

        Hospital hospital = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
        assertThat(hospital.occupiedSlots()).isZero();
        assertThat(hospital.reservedSlots()).isEqualTo(1);
        assertThat(hospital.availableSlots()).isZero();
    }

    @Test
    @DisplayName("HU-11 y HU-12 concurrencia cruzada: entrega y alta simulada simultáneas sobre el mismo hospital no generan deadlock y conservan R, O y D")
    void shouldHandleConcurrentDeliveryAndDischargeOnSameHospitalWithoutDeadlock() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-02", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Compartido", "N02", true, 2));

        // EMG-1 ya entregada -> OCCUPIED en HOSP-01 (O=1)
        Emergency emg1 = emergencyApplicationService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N01", "Caso 1"));
        assertThat(dispatchApplicationService.assignSupervised(emg1.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg1.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        HospitalDeliveryResult del1 = deliveryAndDischargeApplicationService.confirmHospitalDelivery(emg1.id().value());
        assertThat(del1.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
        String occupiedResId = del1.reservation().id();

        // EMG-2 en traslado -> RESERVED en HOSP-01 (R=1, O=1, D=0)
        Emergency emg2 = emergencyApplicationService.registerEmergency(new RegisterEmergencyCommand("INC-B", "N01", "Caso 2"));
        assertThat(dispatchApplicationService.assignSupervised(emg2.id().value(), "AMB-02").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emg2.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Future<HospitalDeliveryResult> deliveryFuture = pool.submit(() -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                return deliveryAndDischargeApplicationService.confirmHospitalDelivery(emg2.id().value());
            });
            Future<HospitalDischargeResult> dischargeFuture = pool.submit(() -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                return deliveryAndDischargeApplicationService.executeSimulatedDischarge(occupiedResId);
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            HospitalDeliveryResult del2 = deliveryFuture.get(15, TimeUnit.SECONDS);
            HospitalDischargeResult dis1 = dischargeFuture.get(15, TimeUnit.SECONDS);

            assertThat(del2.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
            assertThat(dis1.outcome()).isEqualTo(HospitalDischargeOutcome.DISCHARGED);

            Hospital hospital = hospitalRepository.findById(HospitalId.of("HOSP-01")).orElseThrow();
            assertThat(hospital.reservedSlots()).isZero();
            assertThat(hospital.occupiedSlots()).isEqualTo(1);
            assertThat(hospital.availableSlots()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
