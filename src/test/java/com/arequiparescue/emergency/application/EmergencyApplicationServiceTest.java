package com.arequiparescue.emergency.application;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.domain.port.TriageRuleVersionRepository;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyApplicationService — Casos de uso de registro y consulta (HU-01, HU-02)")
class EmergencyApplicationServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private TriageRuleVersionRepository triageRuleVersionRepository;

    @Mock
    private RoadGraphRepository roadGraphRepository;

    private LogicalClock logicalClock;
    private EmergencyApplicationService service;

    @BeforeEach
    void setUp() {
        logicalClock = new LogicalClock(LogicalTime.of(5L));
        service = new EmergencyApplicationService(
                emergencyRepository,
                triageRuleVersionRepository,
                roadGraphRepository,
                logicalClock
        );
    }

    @Test
    @DisplayName("Registra una emergencia válida aplicando triage determinista y sellando el instante lógico actual en REGISTERED")
    void shouldRegisterValidEmergencyWithDeterministicTriageAndLogicalTime() {
        when(roadGraphRepository.loadGraph()).thenReturn(sampleGraph());
        when(triageRuleVersionRepository.findActiveVersion())
                .thenReturn(Optional.of(TriageRuleVersion.initialApprovedCatalog()));
        when(emergencyRepository.nextSequenceNumber()).thenReturn(1L);
        when(emergencyRepository.save(any(Emergency.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Emergency saved = service.registerEmergency(new RegisterEmergencyCommand("INC-D", "N02", ""));

        assertThat(saved.id()).isEqualTo(EmergencyId.of("EMG-00001"));
        assertThat(saved.category().code()).isEqualTo("INC-D");
        assertThat(saved.locationNode()).isEqualTo(NodeId.of("N02"));
        assertThat(saved.description()).isEmpty();
        assertThat(saved.simulatedPriority()).isEqualTo(SimulatedPriority.HIGH);
        assertThat(saved.requiredEquipment()).isEqualTo(EquipmentType.BASIC);
        assertThat(saved.appliedRuleVersionId().value()).isEqualTo("RUL-V01");
        assertThat(saved.state()).isEqualTo(EmergencyState.REGISTERED);
        assertThat(saved.registeredAt()).isEqualTo(LogicalTime.of(5L));
        verify(emergencyRepository).save(any(Emergency.class));
    }

    @Test
    @DisplayName("Rechaza solicitudes inválidas sin persistir datos cuando la categoría o el nodo no son válidos (RN-EME-010)")
    void shouldRejectInvalidCategoryOrNodeWithoutPersisting() {
        assertThatThrownBy(() -> service.registerEmergency(new RegisterEmergencyCommand("INC-X", "N01", "Prueba")))
                .isInstanceOf(EmergencyValidationException.class)
                .satisfies(ex -> assertThat(((EmergencyValidationException) ex).field()).isEqualTo("incidentCategory"));

        assertThatThrownBy(() -> service.registerEmergency(new RegisterEmergencyCommand("INC-A", "N99", "Prueba")))
                .isInstanceOf(EmergencyValidationException.class)
                .satisfies(ex -> assertThat(((EmergencyValidationException) ex).field()).isEqualTo("nodeId"));

        verify(emergencyRepository, never()).save(any());
    }

    private static RoadGraph sampleGraph() {
        List<GraphNode> nodes = List.of(
                new GraphNode(NodeId.of("N01"), "Z01", "N01 — Cercado", false),
                new GraphNode(NodeId.of("N02"), "Z02", "N02 — Yanahuara", false)
        );
        List<GraphEdge> edges = List.of(
                new GraphEdge("E01", NodeId.of("N01"), NodeId.of("N02"), 4)
        );
        return new RoadGraph(nodes, edges);
    }
}
