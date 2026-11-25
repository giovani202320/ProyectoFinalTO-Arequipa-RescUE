package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DispatchApplicationService — Asignación automática y reintentos acotados 1+3 (HU-07)")
class AutomaticDispatchRetryTest {

    private EmergencyRepository emergencyRepository;
    private AmbulanceRepository ambulanceRepository;
    private RoadGraphRepository roadGraphRepository;
    private DispatchTraceabilityRepository traceabilityRepository;
    private SupervisedDispatchAttemptExecutor attemptExecutor;
    private DispatchApplicationService service;

    @BeforeEach
    void setUp() {
        emergencyRepository = mock(EmergencyRepository.class);
        ambulanceRepository = mock(AmbulanceRepository.class);
        roadGraphRepository = mock(RoadGraphRepository.class);
        traceabilityRepository = mock(DispatchTraceabilityRepository.class);
        attemptExecutor = mock(SupervisedDispatchAttemptExecutor.class);
        LogicalClock clock = new LogicalClock(LogicalTime.of(5L));

        when(roadGraphRepository.loadGraph()).thenReturn(new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false)
                ),
                List.of(new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 3))
        ));

        service = new DispatchApplicationService(
                emergencyRepository,
                ambulanceRepository,
                roadGraphRepository,
                traceabilityRepository,
                attemptExecutor,
                clock
        );
    }

    @Test
    @DisplayName("CA-CON-01: Éxito en el intento inicial consume 0 reintentos")
    void shouldAssignOnInitialAttemptWithZeroRetries() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Emergency assignedEmg = registered.assignAmbulance("AMB-01", LogicalTime.of(5L));
        Ambulance assignedAmb = amb1.assignToEmergency(registered.id());

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById()).thenReturn(List.of(amb1));
        when(attemptExecutor.executeAutomaticAssignmentAttempt(registered.id(), amb1.id(), LogicalTime.of(5L)))
                .thenReturn(new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        true, false, assignedEmg, assignedAmb, "Asignada AMB-01"));

        AutomaticDispatchResult result = service.assignAutomatic("EMG-00001");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(result.attemptsExecuted()).isEqualTo(1);
        assertThat(result.retriesConsumed()).isZero();
        verify(attemptExecutor, times(1)).executeAutomaticAssignmentAttempt(any(), any(), any());
    }

    @Test
    @DisplayName("CA-CON-04: Sin elegibles en intento inicial pasa a WAITING_AMBULANCE con 0 reintentos")
    void shouldMoveToWaitingImmediatelyWhenNoEligibleOnInitialAttempt() {
        Emergency registered = emergency("EMG-00001", EquipmentType.SPECIALIZED, "N02");
        Ambulance basicOnly = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Emergency waiting = registered.enterOrRemainInWaitingAmbulance(LogicalTime.of(5L), "NO_ELIGIBLE_AMBULANCE");

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById()).thenReturn(List.of(basicOnly));
        when(attemptExecutor.moveEmergencyToWaitingAmbulance(eq(registered.id()), any(), eq("NO_ELIGIBLE_AMBULANCE")))
                .thenReturn(waiting);

        AutomaticDispatchResult result = service.assignAutomatic("EMG-00001");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE);
        assertThat(result.attemptsExecuted()).isZero();
        assertThat(result.retriesConsumed()).isZero();
        verify(attemptExecutor, never()).executeAutomaticAssignmentAttempt(any(), any(), any());
    }

    @Test
    @DisplayName("CA-CON-02 / CA-CON-08: Reintenta tras conflicto concurrente recalculando y confirma en intento 2")
    void shouldRetryAndConfirmSecondCandidateAfterConcurrentConflict() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance ambBasic = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Ambulance ambSpec = Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.SPECIALIZED, NodeId.of("N01"));
        Emergency assignedEmg = registered.assignAmbulance("AMB-02", LogicalTime.of(5L));
        Ambulance assignedAmb = ambSpec.assignToEmergency(registered.id());

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById())
                .thenReturn(List.of(ambBasic, ambSpec))
                .thenReturn(List.of(ambSpec));
        when(attemptExecutor.executeAutomaticAssignmentAttempt(registered.id(), ambBasic.id(), LogicalTime.of(5L)))
                .thenReturn(new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        false, true, registered, ambBasic, "Conflicto sobre AMB-01"));
        when(attemptExecutor.executeAutomaticAssignmentAttempt(registered.id(), ambSpec.id(), LogicalTime.of(5L)))
                .thenReturn(new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        true, false, assignedEmg, assignedAmb, "Asignada AMB-02"));

        AutomaticDispatchResult result = service.assignAutomatic("EMG-00001");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);
        assertThat(result.attemptsExecuted()).isEqualTo(2);
        assertThat(result.retriesConsumed()).isEqualTo(1);
        assertThat(result.assignedAmbulance().id()).isEqualTo(AmbulanceId.of("AMB-02"));
    }

    @Test
    @DisplayName("CA-CON-05: Termina anticipadamente si tras conflicto ya no quedan elegibles")
    void shouldStopEarlyWhenNoEligibleRemainsAfterConflict() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Emergency waiting = registered.enterOrRemainInWaitingAmbulance(LogicalTime.of(5L), "NO_ELIGIBLE_AMBULANCE");

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById())
                .thenReturn(List.of(amb1))
                .thenReturn(List.of());
        when(attemptExecutor.executeAutomaticAssignmentAttempt(registered.id(), amb1.id(), LogicalTime.of(5L)))
                .thenReturn(new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        false, true, registered, amb1, "Conflicto sobre AMB-01"));
        when(attemptExecutor.moveEmergencyToWaitingAmbulance(eq(registered.id()), any(), eq("NO_ELIGIBLE_AMBULANCE")))
                .thenReturn(waiting);

        AutomaticDispatchResult result = service.assignAutomatic("EMG-00001");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE);
        assertThat(result.attemptsExecuted()).isEqualTo(1);
        verify(attemptExecutor, times(1)).executeAutomaticAssignmentAttempt(any(), any(), any());
    }

    @Test
    @DisplayName("CA-CON-06: Agota los 4 intentos (1+3) por conflictos y registra CONCURRENCY_RETRY_EXHAUSTED")
    void shouldExhaustFourAttemptsAndRecordConcurrencyRetryExhausted() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Emergency waiting = registered.enterOrRemainInWaitingAmbulance(
                LogicalTime.of(5L), "CONCURRENCY_RETRY_EXHAUSTED");

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById()).thenReturn(List.of(amb1));
        when(attemptExecutor.executeAutomaticAssignmentAttempt(eq(registered.id()), eq(amb1.id()), any()))
                .thenReturn(new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        false, true, registered, amb1, "Conflicto concurrente"));
        when(attemptExecutor.moveEmergencyToWaitingAmbulance(eq(registered.id()), any(), eq("CONCURRENCY_RETRY_EXHAUSTED")))
                .thenReturn(waiting);

        AutomaticDispatchResult result = service.assignAutomatic("EMG-00001");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.CONCURRENCY_RETRY_EXHAUSTED);
        assertThat(result.attemptsExecuted()).isEqualTo(4);
        assertThat(result.retriesConsumed()).isEqualTo(3);
        assertThat(result.emergency().state()).isEqualTo(EmergencyState.WAITING_AMBULANCE);
        verify(attemptExecutor, times(4)).executeAutomaticAssignmentAttempt(any(), any(), any());
        verify(traceabilityRepository, times(4)).recordAttempt(
                anyString(), eq("AUTOMATIC_DISPATCH"), eq("EMG-00001"), eq("AMB-01"),
                anyInt(), eq("AMBULANCE_UNAVAILABLE_CONFLICT"), anyString(), anyLong());
        verify(traceabilityRepository).recordOperationResult(
                anyString(), eq("AUTOMATIC_DISPATCH"), eq("EMG-00001"), eq(null),
                eq(4), eq("CONCURRENCY_RETRY_EXHAUSTED"), anyString(), anyLong());
    }

    @Test
    @DisplayName("HU-07 CA-5 / RN-CON-005: Error técnico no concurrente aborta sin consumir reintentos")
    void shouldAbortWithoutRetryingOnNonConcurrencyTechnicalError() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));

        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById()).thenReturn(List.of(amb1));
        when(attemptExecutor.executeAutomaticAssignmentAttempt(eq(registered.id()), eq(amb1.id()), any()))
                .thenThrow(new IllegalStateException("Fallo técnico de conexión"));

        assertThatThrownBy(() -> service.assignAutomatic("EMG-00001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fallo técnico");

        verify(attemptExecutor, times(1)).executeAutomaticAssignmentAttempt(any(), any(), any());
        verify(traceabilityRepository).recordOperationResult(
                anyString(), eq("AUTOMATIC_DISPATCH"), eq("EMG-00001"), eq(null),
                eq(1), eq("TECHNICAL_ERROR"), anyString(), anyLong());
    }

    @Test
    @DisplayName("A2 / ADR-0002: assignSupervised clasifica DataIntegrityViolationException como error técnico y no como conflicto de bloqueo")
    void shouldTreatDataIntegrityViolationInSupervisedDispatchAsTechnicalErrorNotLockConflict() {
        EmergencyId emergencyId = EmergencyId.of("EMG-00001");
        AmbulanceId ambulanceId = AmbulanceId.of("AMB-01");
        when(attemptExecutor.executeSupervisedAssignmentAttempt(eq(emergencyId), eq(ambulanceId), any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Violación de FK/CHECK"));

        assertThatThrownBy(() -> service.assignSupervised("EMG-00001", "AMB-01"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("FK/CHECK");

        verify(attemptExecutor, times(1)).executeSupervisedAssignmentAttempt(any(), any(), any());
        verify(traceabilityRepository).recordAttempt(
                anyString(), eq("SUPERVISED_DISPATCH"), eq("EMG-00001"), eq("AMB-01"),
                eq(1), eq("TECHNICAL_ERROR"), anyString(), anyLong());
        verify(traceabilityRepository).recordOperationResult(
                anyString(), eq("SUPERVISED_DISPATCH"), eq("EMG-00001"), eq(null),
                eq(1), eq("TECHNICAL_ERROR"), anyString(), anyLong());
        verify(traceabilityRepository, never()).recordAttempt(
                anyString(), anyString(), anyString(), any(), anyInt(),
                eq("AMBULANCE_UNAVAILABLE_CONFLICT"), anyString(), anyLong());
    }

    @Test
    @DisplayName("A2 / ADR-0002: assignSupervised mantiene clasificación de conflicto ante PessimisticLockingFailureException")
    void shouldClassifyPessimisticLockFailureInSupervisedDispatchAsConflictReloadRequired() {
        Emergency registered = emergency("EMG-00001", EquipmentType.BASIC, "N02");
        Ambulance amb2 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.BASIC, NodeId.of("N01"));
        when(attemptExecutor.executeSupervisedAssignmentAttempt(eq(registered.id()), eq(AmbulanceId.of("AMB-01")), any()))
                .thenThrow(new org.springframework.dao.CannotAcquireLockException("Lock wait timeout"));
        when(emergencyRepository.findById(registered.id())).thenReturn(Optional.of(registered));
        when(ambulanceRepository.findAllOrderById()).thenReturn(List.of(amb2));

        SupervisedDispatchResult result = service.assignSupervised("EMG-00001", "AMB-01");

        assertThat(result.outcome()).isEqualTo(DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED);
        verify(attemptExecutor, times(1)).executeSupervisedAssignmentAttempt(any(), any(), any());
        verify(traceabilityRepository).recordAttempt(
                anyString(), eq("SUPERVISED_DISPATCH"), eq("EMG-00001"), eq("AMB-01"),
                eq(1), eq("AMBULANCE_UNAVAILABLE_CONFLICT"), anyString(), anyLong());
    }

    private static Emergency emergency(String id, EquipmentType equipment, String node) {
        return Emergency.register(
                EmergencyId.of(id),
                NodeId.of(node),
                "Emergencia " + id,
                new TriageClassification(IncidentCategory.INC_C, SimulatedPriority.MEDIUM,
                        equipment, TriageRuleVersionId.of("RUL-V01")),
                LogicalTime.of(1L)
        );
    }
}
