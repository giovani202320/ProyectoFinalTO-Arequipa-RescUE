package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryResult;
import com.arequiparescue.hospital.application.HospitalDischargeResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WaitingQueueSimulationTrigger — Integración de eventos discretos con colas, llegadas, entregas y altas (HU-08 a HU-12)")
class WaitingQueueSimulationTriggerTest {

    @Test
    @DisplayName("Dispara evaluación de colas, llegadas, entregas y altas al recibir los eventos discretos correspondientes")
    void shouldTriggerQueueEvaluationDeliveryAndDischargeOnRelevantDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        HospitalDispatchApplicationService hospitalService = mock(HospitalDispatchApplicationService.class);
        HospitalDeliveryAndDischargeApplicationService deliveryService =
                mock(HospitalDeliveryAndDischargeApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(
                dispatchService, hospitalService, deliveryService);

        WaitingQueueEvaluationResult expectedAmbulance = new WaitingQueueEvaluationResult(1, 1, 0, List.of(), "OK");
        WaitingHospitalQueueEvaluationResult expectedHospital = new WaitingHospitalQueueEvaluationResult(1, 1, 0, List.of(), "OK");
        HospitalReservationResult expectedArrival = new HospitalReservationResult(
                "OP-1", HospitalDispatchOutcome.IN_TRANSPORT, null, null, null, 1, 0, "IN_TRANSPORT", "OK");
        HospitalDeliveryResult expectedDelivery = new HospitalDeliveryResult(
                "OP-2", HospitalDeliveryOutcome.COMPLETED, null, null, null, null, expectedAmbulance, "OK");
        HospitalDischargeResult expectedDischarge = new HospitalDischargeResult(
                "OP-3", HospitalDischargeOutcome.DISCHARGED, null, null, expectedHospital, "OK");

        when(dispatchService.evaluateWaitingAmbulanceQueue()).thenReturn(expectedAmbulance);
        when(dispatchService.evaluateWaitingAmbulanceQueueForAmbulance("AMB-01")).thenReturn(expectedAmbulance);
        when(hospitalService.evaluateWaitingHospitalQueue()).thenReturn(expectedHospital);
        when(hospitalService.confirmArrivalAndReserveHospital("EMG-00001")).thenReturn(expectedArrival);
        when(deliveryService.confirmHospitalDelivery("EMG-00002")).thenReturn(expectedDelivery);
        when(deliveryService.executeSimulatedDischarge("RES-1")).thenReturn(expectedDischarge);

        Optional<WaitingQueueEvaluationResult> queueEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-1", LogicalTime.of(3L), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "QUEUE", "Evaluar colas"));
        Optional<WaitingQueueEvaluationResult> releaseAmbEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-2", LogicalTime.of(4L), DiscreteEventType.RESOURCE_RELEASE, 2L, "AMB-01", "Ambulancia libre"));
        Optional<WaitingQueueEvaluationResult> releaseHospEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-3", LogicalTime.of(5L), DiscreteEventType.RESOURCE_RELEASE, 3L, "HOSP-01", "Cupo hospitalario liberado"));
        Optional<WaitingQueueEvaluationResult> arrivalEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-4", LogicalTime.of(6L), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 4L, "EMG-00001", "Llegada al incidente"));
        Optional<WaitingQueueEvaluationResult> deliveryEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-5", LogicalTime.of(7L), DiscreteEventType.RESOURCE_RELEASE, 5L, "EMG-00002", "Entrega en hospital"));
        Optional<WaitingQueueEvaluationResult> dischargeEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-6", LogicalTime.of(8L), DiscreteEventType.RESOURCE_RELEASE, 6L, "RES-1", "Alta simulada"));

        assertThat(queueEval).isPresent();
        assertThat(releaseAmbEval).isPresent();
        assertThat(releaseHospEval).isEmpty();
        assertThat(arrivalEval).isEmpty();
        assertThat(deliveryEval).isPresent();
        assertThat(dischargeEval).isEmpty();

        verify(dispatchService).evaluateWaitingAmbulanceQueue();
        verify(dispatchService).evaluateWaitingAmbulanceQueueForAmbulance("AMB-01");
        verify(hospitalService, org.mockito.Mockito.times(2)).evaluateWaitingHospitalQueue();
        verify(hospitalService).confirmArrivalAndReserveHospital("EMG-00001");
        verify(deliveryService).confirmHospitalDelivery("EMG-00002");
        verify(deliveryService).executeSimulatedDischarge("RES-1");
    }

    @Test
    @DisplayName("Ignora eventos discretos ajenos a evaluación de colas, liberación de recursos o llegada al incidente")
    void shouldIgnoreUnrelatedDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        HospitalDispatchApplicationService hospitalService = mock(HospitalDispatchApplicationService.class);
        HospitalDeliveryAndDischargeApplicationService deliveryService =
                mock(HospitalDeliveryAndDischargeApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(
                dispatchService, hospitalService, deliveryService);

        Optional<WaitingQueueEvaluationResult> result = trigger.handleEvent(new DiscreteEvent(
                "EVT-7", LogicalTime.of(1L), DiscreteEventType.EMERGENCY_REGISTRATION, 1L, "EMG-00001", "Registro"));

        assertThat(result).isEmpty();
        verifyNoInteractions(dispatchService);
        verifyNoInteractions(hospitalService);
        verifyNoInteractions(deliveryService);
    }

    @Test
    @DisplayName("Propaga rechazo explícito cuando llegada, entrega o alta devuelven DUPLICATE_OR_INVALID_STATE o prefijo inválido")
    void shouldRejectPreconditionFailuresAndUnknownResourceReleasePrefixes() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        HospitalDispatchApplicationService hospitalService = mock(HospitalDispatchApplicationService.class);
        HospitalDeliveryAndDischargeApplicationService deliveryService =
                mock(HospitalDeliveryAndDischargeApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(
                dispatchService, hospitalService, deliveryService);

        when(hospitalService.confirmArrivalAndReserveHospital("EMG-00025")).thenReturn(new HospitalReservationResult(
                "OP-A", HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE, null, null, null, 0, 0,
                "DUPLICATE_OR_INVALID_STATE", "La emergencia EMG-00025 está en estado REGISTERED."));
        when(deliveryService.confirmHospitalDelivery("EMG-00027")).thenReturn(new HospitalDeliveryResult(
                "OP-D", HospitalDeliveryOutcome.DUPLICATE_OR_INVALID_STATE, null, null, null, null, null,
                "La emergencia EMG-00027 no está en IN_TRANSPORT."));
        when(deliveryService.executeSimulatedDischarge("RES-99")).thenReturn(new HospitalDischargeResult(
                "OP-R", HospitalDischargeOutcome.DUPLICATE_OR_INVALID_STATE, null, null, null,
                "La reserva RES-99 no está en OCCUPIED."));
        when(dispatchService.evaluateWaitingAmbulanceQueue()).thenReturn(
                new WaitingQueueEvaluationResult(0, 0, 3, List.of(), "Sin asignaciones"));

        assertThatThrownBy(() -> trigger.handleEvent(new DiscreteEvent(
                "EV-A", LogicalTime.of(1L), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 1L, "EMG-00025", "Llegada")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REGISTERED");

        assertThatThrownBy(() -> trigger.handleEvent(new DiscreteEvent(
                "EV-D", LogicalTime.of(2L), DiscreteEventType.RESOURCE_RELEASE, 2L, "EMG-00027", "Entrega")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_TRANSPORT");

        assertThatThrownBy(() -> trigger.handleEvent(new DiscreteEvent(
                "EV-R", LogicalTime.of(3L), DiscreteEventType.RESOURCE_RELEASE, 3L, "RES-99", "Alta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OCCUPIED");

        assertThatThrownBy(() -> trigger.handleEvent(new DiscreteEvent(
                "EV-U", LogicalTime.of(4L), DiscreteEventType.RESOURCE_RELEASE, 4L, "12345", "Prefijo inválido")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12345");

        Optional<WaitingQueueEvaluationResult> zeroAssigned = trigger.handleEvent(new DiscreteEvent(
                "EV-Q", LogicalTime.of(5L), DiscreteEventType.WAITING_QUEUE_EVALUATION, 5L, "SYS-QUEUE", "Cola"));
        assertThat(zeroAssigned).isPresent();
        assertThat(zeroAssigned.orElseThrow().assignedCount()).isZero();
        assertThat(zeroAssigned.orElseThrow().remainingWaitingCount()).isEqualTo(3);
    }
}
