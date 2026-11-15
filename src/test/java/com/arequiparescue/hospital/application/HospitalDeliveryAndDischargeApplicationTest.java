package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
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
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.routing.domain.NodeId;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("HospitalDeliveryAndDischargeApplicationTest — Coordinación post-commit e idempotencia (HU-11, HU-12)")
class HospitalDeliveryAndDischargeApplicationTest {

    private EmergencyRepository emergencyRepository;
    private HospitalReservationRepository reservationRepository;
    private DispatchTraceabilityRepository traceabilityRepository;
    private HospitalDeliveryAndDischargeAttemptExecutor attemptExecutor;
    private DispatchApplicationService dispatchApplicationService;
    private HospitalDispatchApplicationService hospitalDispatchApplicationService;
    private LogicalClock logicalClock;
    private HospitalDeliveryAndDischargeApplicationService service;

    @BeforeEach
    void setUp() {
        emergencyRepository = mock(EmergencyRepository.class);
        reservationRepository = mock(HospitalReservationRepository.class);
        traceabilityRepository = mock(DispatchTraceabilityRepository.class);
        attemptExecutor = mock(HospitalDeliveryAndDischargeAttemptExecutor.class);
        dispatchApplicationService = mock(DispatchApplicationService.class);
        hospitalDispatchApplicationService = mock(HospitalDispatchApplicationService.class);
        logicalClock = mock(LogicalClock.class);
        when(logicalClock.currentTime()).thenReturn(LogicalTime.of(10L));

        service = new HospitalDeliveryAndDischargeApplicationService(
                emergencyRepository,
                reservationRepository,
                traceabilityRepository,
                attemptExecutor,
                dispatchApplicationService,
                hospitalDispatchApplicationService,
                logicalClock
        );
    }

    @Test
    @DisplayName("HU-11: entrega exitosa dispara reevaluación WAITING_AMBULANCE tras commit")
    void shouldTriggerWaitingAmbulanceQueueAfterSuccessfulDelivery() {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_B, SimulatedPriority.MEDIUM, EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01"));
        Emergency completed = Emergency.register(
                        EmergencyId.of("EMG-00001"), NodeId.of("N01"), "Caso", triage, LogicalTime.of(1L))
                .assignAmbulance("AMB-01", LogicalTime.of(2L))
                .arriveAtIncident(LogicalTime.of(3L))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(4L))
                .completeHospitalDelivery(LogicalTime.of(10L));
        Ambulance available = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N04"));
        Hospital hospital = Hospital.reconstruct(
                HospitalId.of("HOSP-01"), "Hospital", NodeId.of("N04"), true, 2, 0, 1);
        HospitalReservation reservation = HospitalReservation.reconstruct(
                "RES-1", EmergencyId.of("EMG-00001"), HospitalId.of("HOSP-01"),
                HospitalReservationStatus.OCCUPIED, LogicalTime.of(4L), LogicalTime.of(10L), null);
        WaitingQueueEvaluationResult queueEval = new WaitingQueueEvaluationResult(1, 1, 0, List.of(), "Asignada");

        when(attemptExecutor.executeHospitalDelivery(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalDeliveryAndDischargeAttemptExecutor.DeliveryExecutionOutcome(
                        true, completed, available, hospital, reservation, "Entregada"));
        when(dispatchApplicationService.evaluateWaitingAmbulanceQueueForAmbulance("AMB-01")).thenReturn(queueEval);

        HospitalDeliveryResult result = service.confirmHospitalDelivery("EMG-00001");

        assertThat(result.outcome()).isEqualTo(HospitalDeliveryOutcome.COMPLETED);
        assertThat(result.ambulanceQueueEvaluation()).isEqualTo(queueEval);
        verify(dispatchApplicationService).evaluateWaitingAmbulanceQueueForAmbulance("AMB-01");
        verify(traceabilityRepository).recordOperationResult(
                anyString(), eq("HOSPITAL_DELIVERY"), eq("EMG-00001"), eq("HOSP-01"),
                eq(1), eq("COMPLETED"), anyString(), eq(10L));
    }

    @Test
    @DisplayName("HU-11: entrega duplicada o fallo transaccional no dispara evaluación de cola")
    void shouldNotTriggerQueueEvaluationOnDuplicateDeliveryOrFailure() {
        when(attemptExecutor.executeHospitalDelivery(EmergencyId.of("EMG-00001"), LogicalTime.of(10L)))
                .thenReturn(new HospitalDeliveryAndDischargeAttemptExecutor.DeliveryExecutionOutcome(
                        false, null, null, null, null, "Duplicada"));

        HospitalDeliveryResult dup = service.confirmHospitalDelivery("EMG-00001");
        assertThat(dup.outcome()).isEqualTo(HospitalDeliveryOutcome.DUPLICATE_OR_INVALID_STATE);
        verifyNoInteractions(dispatchApplicationService);

        when(attemptExecutor.executeHospitalDelivery(EmergencyId.of("EMG-00002"), LogicalTime.of(10L)))
                .thenThrow(new DataAccessResourceFailureException("Fallo DB"));
        assertThatThrownBy(() -> service.confirmHospitalDelivery("EMG-00002"))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verifyNoInteractions(dispatchApplicationService);
    }

    @Test
    @DisplayName("HU-12: alta simulada válida dispara reevaluación WAITING_HOSPITAL; alta duplicada es idempotente sin reevaluar")
    void shouldTriggerWaitingHospitalQueueOnDischargeAndSkipOnDuplicate() {
        Hospital hospital = Hospital.reconstruct(
                HospitalId.of("HOSP-01"), "Hospital", NodeId.of("N04"), true, 2, 0, 0);
        HospitalReservation discharged = HospitalReservation.reconstruct(
                "RES-1", EmergencyId.of("EMG-00001"), HospitalId.of("HOSP-01"),
                HospitalReservationStatus.DISCHARGED, LogicalTime.of(4L), LogicalTime.of(6L), LogicalTime.of(10L));
        WaitingHospitalQueueEvaluationResult hospEval =
                new WaitingHospitalQueueEvaluationResult(1, 1, 0, List.of(), "Reservada");

        when(reservationRepository.findById("RES-1")).thenReturn(Optional.of(discharged));
        when(attemptExecutor.executeSimulatedDischarge("RES-1", HospitalId.of("HOSP-01"), LogicalTime.of(10L)))
                .thenReturn(new HospitalDeliveryAndDischargeAttemptExecutor.DischargeExecutionOutcome(
                        true, hospital, discharged, "Alta OK"))
                .thenReturn(new HospitalDeliveryAndDischargeAttemptExecutor.DischargeExecutionOutcome(
                        false, null, discharged, "Duplicada"));
        when(hospitalDispatchApplicationService.evaluateWaitingHospitalQueue()).thenReturn(hospEval);

        HospitalDischargeResult first = service.executeSimulatedDischarge("RES-1");
        assertThat(first.outcome()).isEqualTo(HospitalDischargeOutcome.DISCHARGED);
        assertThat(first.hospitalQueueEvaluation()).isEqualTo(hospEval);
        verify(hospitalDispatchApplicationService).evaluateWaitingHospitalQueue();

        HospitalDischargeResult second = service.executeSimulatedDischarge("RES-1");
        assertThat(second.outcome()).isEqualTo(HospitalDischargeOutcome.DUPLICATE_OR_INVALID_STATE);
        assertThat(second.hospitalQueueEvaluation()).isNull();
    }
}
