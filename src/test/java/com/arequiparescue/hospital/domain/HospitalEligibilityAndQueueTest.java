package com.arequiparescue.hospital.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HospitalEligibilityPolicy, WaitingHospitalQueuePolicy y transiciones de dominio (HU-09, HU-10)")
class HospitalEligibilityAndQueueTest {

    private static RoadGraph sampleGraph() {
        List<GraphNode> nodes = List.of(
                new GraphNode(NodeId.of("N01"), "Nodo 1", "Z1", false),
                new GraphNode(NodeId.of("N02"), "Nodo 2", "Z2", false),
                new GraphNode(NodeId.of("N03"), "Nodo 3", "Z3", false),
                new GraphNode(NodeId.of("N10"), "Nodo aislado", "Z10", true)
        );
        List<GraphEdge> edges = List.of(
                new GraphEdge("E1", NodeId.of("N01"), NodeId.of("N02"), 5),
                new GraphEdge("E2", NodeId.of("N01"), NodeId.of("N03"), 5)
        );
        return new RoadGraph(nodes, edges);
    }

    private static Emergency assignedEmergency(String id, String node) {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01"));
        Emergency emergency = Emergency.register(
                EmergencyId.of(id), NodeId.of(node), "Paciente crítico", triage, LogicalTime.of(1L));
        return emergency.assignAmbulance("AMB-01", LogicalTime.of(2L));
    }

    @Test
    @DisplayName("Hospital.reserveSlot incrementa R manteniendo R + O <= C y rechaza sin cupos o deshabilitado")
    void shouldReserveSlotRespectingCapacityInvariants() {
        Hospital h = Hospital.reconstruct(
                HospitalId.of("HOSP-01"), "Hospital Central", NodeId.of("N02"), true, 2, 1, 0);
        Hospital reserved = h.reserveSlot();
        assertThat(reserved.reservedSlots()).isEqualTo(2);
        assertThat(reserved.availableSlots()).isZero();
        assertThatThrownBy(reserved::reserveSlot).isInstanceOf(IllegalStateException.class);

        Hospital disabled = Hospital.registerInCatalog(
                HospitalId.of("HOSP-02"), "Hospital Inactivo", NodeId.of("N02"), false, 3);
        assertThatThrownBy(disabled::reserveSlot).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Transiciones de dominio HU-09: ASSIGNED -> AT_INCIDENT -> IN_TRANSPORT o WAITING_HOSPITAL reteniendo ambulancia")
    void shouldTransitionEmergencyAndAmbulanceAcrossArrivalAndHospitalOutcome() {
        Emergency assigned = assignedEmergency("EMG-00001", "N01");
        Ambulance ambAssigned = Ambulance.registerInCatalog(
                        AmbulanceId.of("AMB-01"), EquipmentType.SPECIALIZED, NodeId.of("N02"))
                .assignToEmergency(EmergencyId.of("EMG-00001"));

        Emergency atIncident = assigned.arriveAtIncident(LogicalTime.of(3L));
        Ambulance ambAtIncident = ambAssigned.arriveAtIncident(atIncident.locationNode());
        assertThat(atIncident.state()).isEqualTo(EmergencyState.AT_INCIDENT);
        assertThat(atIncident.atIncidentAt()).isEqualTo(LogicalTime.of(3L));
        assertThat(ambAtIncident.state()).isEqualTo(AmbulanceState.AT_INCIDENT);
        assertThat(ambAtIncident.currentNode()).isEqualTo(NodeId.of("N01"));

        Emergency inTransport = atIncident.reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(4L));
        Ambulance ambTransporting = ambAtIncident.startTransportToHospital();
        assertThat(inTransport.state()).isEqualTo(EmergencyState.IN_TRANSPORT);
        assertThat(inTransport.reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(ambTransporting.state()).isEqualTo(AmbulanceState.TRANSPORTING);

        Emergency waiting = atIncident.enterOrRemainInWaitingHospital(LogicalTime.of(5L), "NO_ELIGIBLE_HOSPITAL");
        assertThat(waiting.state()).isEqualTo(EmergencyState.WAITING_HOSPITAL);
        assertThat(waiting.hospitalQueueEnteredAt()).isEqualTo(LogicalTime.of(5L));
        assertThat(waiting.assignedAmbulanceId()).isEqualTo("AMB-01");

        // Reevaluación posterior no debe sobrescribir hospitalQueueEnteredAt original (CA-HOS-09)
        Emergency stillWaiting = waiting.enterOrRemainInWaitingHospital(LogicalTime.of(10L), "STILL_FULL");
        assertThat(stillWaiting.hospitalQueueEnteredAt()).isEqualTo(LogicalTime.of(5L));
    }

    @Test
    @DisplayName("HospitalEligibilityPolicy filtra deshabilitados/D=0/inalcanzables y ordena por costo ASC, hospitalId ASC")
    void shouldFilterAndSortEligibleHospitalsDeterministically() {
        RoadGraph graph = sampleGraph();
        Emergency emergency = assignedEmergency("EMG-00001", "N01").arriveAtIncident(LogicalTime.of(3L));

        Hospital full = Hospital.reconstruct(
                HospitalId.of("HOSP-00"), "Lleno", NodeId.of("N02"), true, 1, 1, 0);
        Hospital disabled = Hospital.registerInCatalog(
                HospitalId.of("HOSP-01"), "Inactivo", NodeId.of("N02"), false, 2);
        Hospital tieB = Hospital.registerInCatalog(
                HospitalId.of("HOSP-03"), "Hospital B", NodeId.of("N03"), true, 2);
        Hospital tieA = Hospital.registerInCatalog(
                HospitalId.of("HOSP-02"), "Hospital A", NodeId.of("N02"), true, 2);
        Hospital isolated = Hospital.registerInCatalog(
                HospitalId.of("HOSP-04"), "Aislado", NodeId.of("N10"), true, 2);

        HospitalEligibilityPolicy policy = new HospitalEligibilityPolicy();
        List<EligibleHospitalCandidate> candidates = policy.evaluateEligibleHospitals(
                emergency, List.of(full, disabled, tieB, tieA, isolated), graph);

        assertThat(candidates).extracting(c -> c.hospital().id().value())
                .containsExactly("HOSP-02", "HOSP-03");
        assertThat(candidates).allMatch(c -> c.routeCost() == 5);
    }

    @Test
    @DisplayName("WaitingHospitalQueuePolicy ordena estrictamente FIFO por hospitalQueueEnteredAt ASC y luego id ASC")
    void shouldSortWaitingHospitalQueueStrictlyFifo() {
        Emergency e1 = assignedEmergency("EMG-00002", "N01")
                .arriveAtIncident(LogicalTime.of(2L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(4L), "FULL");
        Emergency e2 = assignedEmergency("EMG-00001", "N01")
                .arriveAtIncident(LogicalTime.of(2L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(4L), "FULL");
        Emergency e3 = assignedEmergency("EMG-00003", "N01")
                .arriveAtIncident(LogicalTime.of(2L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(2L), "FULL");

        WaitingHospitalQueuePolicy policy = new WaitingHospitalQueuePolicy();
        List<Emergency> sorted = policy.sortFifo(List.of(e1, e2, e3));

        assertThat(sorted).extracting(e -> e.id().value())
                .containsExactly("EMG-00003", "EMG-00001", "EMG-00002");
    }
}
