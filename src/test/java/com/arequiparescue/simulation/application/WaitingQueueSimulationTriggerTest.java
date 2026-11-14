package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WaitingQueueSimulationTrigger — Integración de eventos discretos con colas y llegadas (HU-08, HU-09, HU-10)")
class WaitingQueueSimulationTriggerTest {

    @Test
    @DisplayName("Dispara evaluación de colas al recibir WAITING_QUEUE_EVALUATION, RESOURCE_RELEASE de ambulancia y hospital, y AMBULANCE_ARRIVAL_AT_INCIDENT")
    void shouldTriggerQueueEvaluationAndArrivalOnRelevantDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        HospitalDispatchApplicationService hospitalService = mock(HospitalDispatchApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(dispatchService, hospitalService);

        WaitingQueueEvaluationResult expectedAmbulance = new WaitingQueueEvaluationResult(1, 1, 0, List.of(), "OK");
        WaitingHospitalQueueEvaluationResult expectedHospital = new WaitingHospitalQueueEvaluationResult(1, 1, 0, List.of(), "OK");
        HospitalReservationResult expectedArrival = new HospitalReservationResult(
                "OP-1", HospitalDispatchOutcome.IN_TRANSPORT, null, null, null, 1, 0, "IN_TRANSPORT", "OK");

        when(dispatchService.evaluateWaitingAmbulanceQueue()).thenReturn(expectedAmbulance);
        when(dispatchService.evaluateWaitingAmbulanceQueueForAmbulance("AMB-01")).thenReturn(expectedAmbulance);
        when(hospitalService.evaluateWaitingHospitalQueue()).thenReturn(expectedHospital);
        when(hospitalService.confirmArrivalAndReserveHospital("EMG-00001")).thenReturn(expectedArrival);

        Optional<WaitingQueueEvaluationResult> queueEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-1", LogicalTime.of(3L), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "QUEUE", "Evaluar colas"));
        Optional<WaitingQueueEvaluationResult> releaseAmbEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-2", LogicalTime.of(4L), DiscreteEventType.RESOURCE_RELEASE, 2L, "AMB-01", "Ambulancia libre"));
        Optional<WaitingQueueEvaluationResult> releaseHospEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-3", LogicalTime.of(5L), DiscreteEventType.RESOURCE_RELEASE, 3L, "HOSP-01", "Cupo hospitalario liberado"));
        Optional<WaitingQueueEvaluationResult> arrivalEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-4", LogicalTime.of(6L), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 4L, "EMG-00001", "Llegada al incidente"));

        assertThat(queueEval).isPresent();
        assertThat(releaseAmbEval).isPresent();
        assertThat(releaseHospEval).isEmpty();
        assertThat(arrivalEval).isEmpty();
        verify(dispatchService).evaluateWaitingAmbulanceQueue();
        verify(dispatchService).evaluateWaitingAmbulanceQueueForAmbulance("AMB-01");
        verify(hospitalService, org.mockito.Mockito.times(2)).evaluateWaitingHospitalQueue();
        verify(hospitalService).confirmArrivalAndReserveHospital("EMG-00001");
    }

    @Test
    @DisplayName("Ignora eventos discretos ajenos a evaluación de colas, liberación de recursos o llegada al incidente")
    void shouldIgnoreUnrelatedDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        HospitalDispatchApplicationService hospitalService = mock(HospitalDispatchApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(dispatchService, hospitalService);

        Optional<WaitingQueueEvaluationResult> result = trigger.handleEvent(new DiscreteEvent(
                "EVT-5", LogicalTime.of(1L), DiscreteEventType.EMERGENCY_REGISTRATION, 1L, "EMG-00001", "Registro"));

        assertThat(result).isEmpty();
        verifyNoInteractions(dispatchService);
        verifyNoInteractions(hospitalService);
    }
}
