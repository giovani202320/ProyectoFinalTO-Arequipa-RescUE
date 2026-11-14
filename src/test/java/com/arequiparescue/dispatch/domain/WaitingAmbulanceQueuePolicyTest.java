package com.arequiparescue.dispatch.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.routing.domain.GraphEdge;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WaitingAmbulanceQueuePolicy — Orden FIFO, equipamiento y preservación (HU-08)")
class WaitingAmbulanceQueuePolicyTest {

    private final WaitingAmbulanceQueuePolicy policy = new WaitingAmbulanceQueuePolicy();
    private final RoadGraph graph = new RoadGraph(
            List.of(
                    new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                    new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false),
                    new GraphNode(NodeId.of("N03"), "Z03", "Yanahuara", false),
                    new GraphNode(NodeId.of("N10"), "Z10", "Aislada", true)
            ),
            List.of(
                    new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 3),
                    new GraphEdge("EDG-02", NodeId.of("N01"), NodeId.of("N03"), 5)
            )
    );

    @Test
    @DisplayName("Respeta orden FIFO estricto sin adelantar por prioridad clínica informativa (RN-COL-003, RN-COL-004)")
    void shouldRespectStrictFifoIgnoringInformativePriority() {
        Emergency lowEarlier = waitingEmergency("EMG-00001", IncidentCategory.INC_D,
                SimulatedPriority.LOW, EquipmentType.BASIC, "N02", 1L);
        Emergency highLater = waitingEmergency("EMG-00002", IncidentCategory.INC_B,
                SimulatedPriority.HIGH, EquipmentType.BASIC, "N02", 3L);

        Ambulance basicUnit = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));

        Optional<Emergency> selected = policy.selectNextEmergencyForAvailableAmbulance(
                basicUnit, List.of(highLater, lowEarlier), graph);

        assertThat(selected).isPresent();
        assertThat(selected.get().id()).isEqualTo(EmergencyId.of("EMG-00001"));
    }

    @Test
    @DisplayName("Al liberarse SPECIALIZED prioriza grupo SPECIALIZED sobre BASIC más antiguo (RN-COL-001)")
    void shouldPrioritizeSpecializedGroupWhenSpecializedAmbulanceBecomesAvailable() {
        Emergency basicOlder = waitingEmergency("EMG-00001", IncidentCategory.INC_C,
                SimulatedPriority.MEDIUM, EquipmentType.BASIC, "N02", 1L);
        Emergency specializedNewer = waitingEmergency("EMG-00002", IncidentCategory.INC_A,
                SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, "N03", 5L);

        Ambulance specializedUnit = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-02"), EquipmentType.SPECIALIZED, NodeId.of("N01"));

        Optional<Emergency> selected = policy.selectNextEmergencyForAvailableAmbulance(
                specializedUnit, List.of(basicOlder, specializedNewer), graph);

        assertThat(selected).isPresent();
        assertThat(selected.get().id()).isEqualTo(EmergencyId.of("EMG-00002"));
    }

    @Test
    @DisplayName("Al liberarse SPECIALIZED atiende grupo BASIC supletoriamente si no hay SPECIALIZED alcanzable (RN-COL-002)")
    void shouldServeBasicSupplementallyWhenNoReachableSpecializedWaiting() {
        Emergency specializedIsolated = waitingEmergency("EMG-00001", IncidentCategory.INC_A,
                SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, "N10", 1L);
        Emergency basicWaiting = waitingEmergency("EMG-00002", IncidentCategory.INC_D,
                SimulatedPriority.LOW, EquipmentType.BASIC, "N02", 2L);

        Ambulance specializedUnit = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-02"), EquipmentType.SPECIALIZED, NodeId.of("N01"));

        Optional<Emergency> selected = policy.selectNextEmergencyForAvailableAmbulance(
                specializedUnit, List.of(specializedIsolated, basicWaiting), graph);

        assertThat(selected).isPresent();
        assertThat(selected.get().id()).isEqualTo(EmergencyId.of("EMG-00002"));
    }

    @Test
    @DisplayName("Al liberarse BASIC nunca atiende emergencias SPECIALIZED (RN-COL-007, CA-SIM-01)")
    void shouldNotAssignBasicAmbulanceToSpecializedEmergencies() {
        Emergency specializedWaiting = waitingEmergency("EMG-00001", IncidentCategory.INC_A,
                SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, "N02", 1L);

        Ambulance basicUnit = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));

        Optional<Emergency> selected = policy.selectNextEmergencyForAvailableAmbulance(
                basicUnit, List.of(specializedWaiting), graph);

        assertThat(selected).isEmpty();
    }

    private static Emergency waitingEmergency(
            String id,
            IncidentCategory category,
            SimulatedPriority priority,
            EquipmentType equipment,
            String node,
            long ticks
    ) {
        Emergency registered = Emergency.register(
                EmergencyId.of(id),
                NodeId.of(node),
                "Simulada " + id,
                new TriageClassification(category, priority, equipment, TriageRuleVersionId.of("RUL-V01")),
                LogicalTime.of(ticks)
        );
        return registered.enterOrRemainInWaitingAmbulance(LogicalTime.of(ticks), "NO_ELIGIBLE_AMBULANCE");
    }
}
