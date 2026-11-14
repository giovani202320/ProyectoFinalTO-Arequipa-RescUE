package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.routing.domain.GraphEdge;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HospitalDispatchApplicationService — Llegada, reintentos 1+2 y cola WAITING_HOSPITAL (HU-09, HU-10)")
class HospitalDispatchRetryTest {

    private EmergencyRepository emergencyRepository;
    private AmbulanceRepository ambulanceRepository;
    private HospitalRepository hospitalRepository;
    private RoadGraphRepository roadGraphRepository;
    private DispatchTraceabilityRepository traceabilityRepository;
    private HospitalReservationAttemptExecutor attemptExecutor;
    private LogicalClock logicalClock;
    private HospitalDispatchApplicationService service;

    @BeforeEach
    void setUp() {
        emergencyRepository = mock(EmergencyRepository.class);
        ambulanceRepository = mock(AmbulanceRepository.class);
        hospitalRepository = mock(HospitalRepository.class);
        roadGraphRepository = mock(RoadGraphRepository.class);
        traceabilityRepository = mock(DispatchTraceabilityRepository.class);
        attemptExecutor = mock(HospitalReservationAttemptExecutor.class);
        logicalClock = mock(LogicalClock.class);
        when(logicalClock.currentTime()).thenReturn(LogicalTime.of(10L));
        service = new HospitalDispatchApplicationService(
                emergencyRepository,
                ambulanceRepository,
                hospitalRepository,
                roadGraphRepository,
                traceabilityRepository,
                attemptExecutor,
                logicalClock
        );
    }

    private static RoadGraph graph() {
        return new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "N01", "Z1", false),
                        new GraphNode(NodeId.of("N02"), "N02", "Z2", false),
                        new GraphNode(NodeId.of("N10"), "N10", "Z10", true)
                ),
                List.of(new GraphEdge("E1", NodeId.of("N01"), NodeId.of("N02"), 4))
        );
    }

    private static Emergency assignedEmergency(String id, String node) {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_B, SimulatedPriority.HIGH, EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01"));
        return Emergency.register(EmergencyId.of(id), NodeId.of(node), "Emergencia", triage, LogicalTime.of(1L))
                .assignAmbulance("AMB-01", LogicalTime.of(2L));
    }

    @Test
    @DisplayName("HU-09 éxito en intento 1 consume 0 reintentos y pasa a IN_TRANSPORT")
    void shouldSucceedOnFirstAttemptWithZeroRetries() {
        Emergency atIncident = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(10L));
        Emergency inTransport = atIncident.reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(10L));
        Ambulance ambAtIncident = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"))
                .assignToEmergency(EmergencyId.of("EMG-00001"))
                .arriveAtIncident(NodeId.of("N01"));
        Ambulance ambTransporting = ambAtIncident.startTransportToHospital();
        Hospital hosp = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 2);

        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(true, false, atIncident, ambAtIncident, "OK"));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(atIncident));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(hosp));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.executeHospitalReservationAttempt(
                EmergencyId.of("EMG-00001"), HospitalId.of("HOSP-01"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        true, false, inTransport, ambTransporting, hosp.reserveSlot(), "OK"));

        HospitalReservationResult result = service.confirmArrivalAndReserveHospital("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        assertThat(result.attemptsExecuted()).isEqualTo(1);
        assertThat(result.retriesConsumed()).isZero();
        assertThat(result.reservedHospital().id().value()).isEqualTo("HOSP-01");
        verify(attemptExecutor, times(1)).executeHospitalReservationAttempt(any(), any(), any());
    }

    @Test
    @DisplayName("HU-09 rechaza evento de llegada duplicado sin consumir intentos ni crear reserva")
    void shouldRejectDuplicateArrivalIdempotently() {
        Emergency alreadyInTransport = assignedEmergency("EMG-00001", "N01")
                .arriveAtIncident(LogicalTime.of(5L))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(6L));
        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(
                        false, true, alreadyInTransport, null, "Duplicado"));

        HospitalReservationResult result = service.confirmArrivalAndReserveHospital("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE);
        assertThat(result.attemptsExecuted()).isZero();
    }

    @Test
    @DisplayName("HU-09 sin hospitales elegibles en intento 1 pasa a WAITING_HOSPITAL con 0 reintentos")
    void shouldMoveToWaitingHospitalImmediatelyWhenNoEligibleHospitals() {
        Emergency atIncident = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(10L));
        Emergency waiting = atIncident.enterOrRemainInWaitingHospital(LogicalTime.of(10L), "NO_ELIGIBLE_HOSPITAL");
        Hospital full = Hospital.reconstruct(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 1, 1, 0);

        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(true, false, atIncident, null, "OK"));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(atIncident));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(full));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.moveEmergencyToWaitingHospital(eq(EmergencyId.of("EMG-00001")), any(), anyString()))
                .thenReturn(waiting);

        HospitalReservationResult result = service.confirmArrivalAndReserveHospital("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL);
        assertThat(result.attemptsExecuted()).isZero();
        assertThat(result.retriesConsumed()).isZero();
    }

    @Test
    @DisplayName("HU-09 conflicto en intento 1 y éxito en reintento 2 consume 1 reintento")
    void shouldRetryAndSucceedOnSecondAttemptAfterConflict() {
        Emergency atIncident = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(10L));
        Emergency inTransport = atIncident.reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(10L));
        Hospital hosp = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 1);

        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(true, false, atIncident, null, "OK"));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(atIncident));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(hosp));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.executeHospitalReservationAttempt(any(), eq(HospitalId.of("HOSP-01")), any()))
                .thenReturn(new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        false, true, atIncident, null, hosp, "Sin cupo concurrente"))
                .thenReturn(new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        true, false, inTransport, null, hosp.reserveSlot(), "OK"));

        HospitalReservationResult result = service.confirmArrivalAndReserveHospital("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        assertThat(result.attemptsExecuted()).isEqualTo(2);
        assertThat(result.retriesConsumed()).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-09 agota 3 intentos (1+2) por conflicto y emite HOSPITAL_RESERVATION_RETRY_EXHAUSTED")
    void shouldExhaustRetriesAfterThreeConflictsAndEmitExhaustedOutcome() {
        Emergency atIncident = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(10L));
        Emergency waiting = atIncident.enterOrRemainInWaitingHospital(LogicalTime.of(10L), "HOSPITAL_RESERVATION_RETRY_EXHAUSTED");
        Hospital hosp = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 1);

        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(true, false, atIncident, null, "OK"));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(atIncident));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(hosp));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.executeHospitalReservationAttempt(any(), eq(HospitalId.of("HOSP-01")), any()))
                .thenReturn(new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        false, true, atIncident, null, hosp, "Conflicto"));
        when(attemptExecutor.moveEmergencyToWaitingHospital(
                EmergencyId.of("EMG-00001"), LogicalTime.of(10L), "HOSPITAL_RESERVATION_RETRY_EXHAUSTED"))
                .thenReturn(waiting);

        HospitalReservationResult result = service.confirmArrivalAndReserveHospital("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDispatchOutcome.HOSPITAL_RESERVATION_RETRY_EXHAUSTED);
        assertThat(result.attemptsExecuted()).isEqualTo(3);
        assertThat(result.retriesConsumed()).isEqualTo(2);
        assertThat(result.outcomeCode()).isEqualTo("HOSPITAL_RESERVATION_RETRY_EXHAUSTED");
        verify(attemptExecutor, times(3)).executeHospitalReservationAttempt(any(), any(), any());
    }

    @Test
    @DisplayName("Error técnico no concurrencial aborta sin consumir reintentos")
    void shouldAbortWithoutRetryingOnNonConcurrencyTechnicalError() {
        Emergency atIncident = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(10L));
        Hospital hosp = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 1);

        when(attemptExecutor.confirmArrivalAtIncident(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ArrivalOutcome(true, false, atIncident, null, "OK"));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(atIncident));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(hosp));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.executeHospitalReservationAttempt(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("Fallo técnico DB"));

        assertThatThrownBy(() -> service.confirmArrivalAndReserveHospital("EMG-00001"))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verify(attemptExecutor, times(1)).executeHospitalReservationAttempt(any(), any(), any());
        verify(traceabilityRepository).recordOperationResult(
                anyString(), anyString(), eq("EMG-00001"), eq(null), eq(1), eq("TECHNICAL_ERROR"), anyString(), anyLong());
    }

    @Test
    @DisplayName("HU-10 CA-HOS-09: si primera emergencia en WAITING_HOSPITAL es inalcanzable, conserva su posición y atiende la siguiente alcanzable")
    void shouldKeepUnreachableFirstWaitingEmergencyInPositionAndServeNextReachable() {
        Emergency unreachableFirst = assignedEmergency("EMG-00001", "N10")
                .arriveAtIncident(LogicalTime.of(2L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(3L), "NO_ELIGIBLE_HOSPITAL");
        Emergency reachableSecond = assignedEmergency("EMG-00002", "N01")
                .arriveAtIncident(LogicalTime.of(4L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(5L), "NO_ELIGIBLE_HOSPITAL");
        Emergency reachableInTransport = reachableSecond.reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(10L));
        Hospital hosp = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "H1", NodeId.of("N02"), true, 1);

        when(emergencyRepository.findWaitingHospitalOrderByHospitalQueueEnteredAtAndId())
                .thenReturn(List.of(unreachableFirst, reachableSecond))
                .thenReturn(List.of(unreachableFirst));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00001"))).thenReturn(Optional.of(unreachableFirst));
        when(emergencyRepository.findById(EmergencyId.of("EMG-00002"))).thenReturn(Optional.of(reachableSecond));
        when(hospitalRepository.findAllOrderById()).thenReturn(List.of(hosp));
        when(roadGraphRepository.loadGraph()).thenReturn(graph());
        when(attemptExecutor.executeHospitalReservationAttempt(
                EmergencyId.of("EMG-00002"), HospitalId.of("HOSP-01"), LogicalTime.of(10L)))
                .thenReturn(new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        true, false, reachableInTransport, null, hosp.reserveSlot(), "OK"));

        WaitingHospitalQueueEvaluationResult eval = service.evaluateWaitingHospitalQueue();

        assertThat(eval.evaluatedWaitingCount()).isEqualTo(2);
        assertThat(eval.reservedCount()).isEqualTo(1);
        assertThat(eval.remainingWaitingCount()).isEqualTo(1);
        verify(attemptExecutor, times(1)).executeHospitalReservationAttempt(
                eq(EmergencyId.of("EMG-00002")), eq(HospitalId.of("HOSP-01")), any());
        verify(attemptExecutor, times(0)).executeHospitalReservationAttempt(
                eq(EmergencyId.of("EMG-00001")), any(), any());
        verify(attemptExecutor, times(0)).moveEmergencyToWaitingHospital(eq(EmergencyId.of("EMG-00001")), any(), anyString());
    }
}
