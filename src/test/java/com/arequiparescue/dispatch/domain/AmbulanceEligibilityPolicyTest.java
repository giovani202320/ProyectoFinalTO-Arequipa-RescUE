package com.arequiparescue.dispatch.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dominio puro — AmbulanceEligibilityPolicy (HU-05: preservación, Dijkstra y desempate)")
class AmbulanceEligibilityPolicyTest {

    private final AmbulanceEligibilityPolicy policy = new AmbulanceEligibilityPolicy();

    private static RoadGraph sampleGraph() {
        return new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false),
                        new GraphNode(NodeId.of("N03"), "Z03", "Yanahuara", false),
                        new GraphNode(NodeId.of("N10"), "Z10", "Zona Aislada", true)
                ),
                List.of(
                        new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 5),
                        new GraphEdge("EDG-02", NodeId.of("N01"), NodeId.of("N03"), 2)
                )
        );
    }

    private static Emergency emergencyAt(String nodeCode, EquipmentType equipment, SimulatedPriority priority) {
        return Emergency.register(
                EmergencyId.of("EMG-00001"),
                NodeId.of(nodeCode),
                "Incidente simulado",
                new TriageClassification(
                        equipment == EquipmentType.SPECIALIZED ? IncidentCategory.INC_A : IncidentCategory.INC_B,
                        priority,
                        equipment,
                        TriageRuleVersionId.of("RUL-V01")
                ),
                LogicalTime.of(0)
        );
    }

    @Test
    @DisplayName("HU-05 CA-1: emergencia SPECIALIZED excluye toda unidad BASIC y unidades no AVAILABLE")
    void shouldFilterOnlyAvailableSpecializedForSpecializedEmergency() {
        Emergency emergency = emergencyAt("N01", EquipmentType.SPECIALIZED, SimulatedPriority.HIGH);
        List<Ambulance> catalog = List.of(
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01")),
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.SPECIALIZED, NodeId.of("N02")),
                Ambulance.reconstruct(
                        AmbulanceId.of("AMB-03"),
                        EquipmentType.SPECIALIZED,
                        NodeId.of("N01"),
                        AmbulanceState.ASSIGNED,
                        EmergencyId.of("EMG-00099")
                )
        );

        List<EligibleAmbulanceCandidate> result =
                policy.evaluateEligibleCandidates(emergency, catalog, sampleGraph());

        assertThat(result).extracting(c -> c.ambulance().id().value()).containsExactly("AMB-02");
        assertThat(result.get(0).routeCost()).isEqualTo(5);
    }

    @Test
    @DisplayName("HU-05 CA-2: emergencia BASIC prefiere BASIC preservando SPECIALIZED, y usa SPECIALIZED solo si no hay BASIC")
    void shouldPreserveSpecializedWhenBasicAvailableAndFallbackOtherwise() {
        Emergency emergency = emergencyAt("N01", EquipmentType.BASIC, SimulatedPriority.LOW);
        Ambulance basic = Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.BASIC, NodeId.of("N02"));
        Ambulance spec = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.SPECIALIZED, NodeId.of("N01"));

        List<EligibleAmbulanceCandidate> withBasic =
                policy.evaluateEligibleCandidates(emergency, List.of(spec, basic), sampleGraph());
        assertThat(withBasic).extracting(c -> c.ambulance().id().value()).containsExactly("AMB-02");
        assertThat(withBasic.get(0).supplementalSpecialized()).isFalse();

        List<EligibleAmbulanceCandidate> withoutBasic =
                policy.evaluateEligibleCandidates(emergency, List.of(spec), sampleGraph());
        assertThat(withoutBasic).extracting(c -> c.ambulance().id().value()).containsExactly("AMB-01");
        assertThat(withoutBasic.get(0).supplementalSpecialized()).isTrue();
    }

    @Test
    @DisplayName("HU-05 CA-3: ordena por menor costo Dijkstra y desempata ascendentemente por ID estable (CA-SIM-02)")
    void shouldSortByDijkstraCostAndTieBreakByCanonicalId() {
        Emergency emergency = emergencyAt("N01", EquipmentType.BASIC, SimulatedPriority.LOW);
        List<Ambulance> catalog = List.of(
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-03"), EquipmentType.BASIC, NodeId.of("N02")), // costo 5
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.BASIC, NodeId.of("N03")), // costo 2
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N03"))  // costo 2
        );

        List<EligibleAmbulanceCandidate> result =
                policy.evaluateEligibleCandidates(emergency, catalog, sampleGraph());

        assertThat(result).extracting(c -> c.ambulance().id().value())
                .containsExactly("AMB-01", "AMB-02", "AMB-03");
    }

    @Test
    @DisplayName("HU-05 CA-4: descarta unidades sin camino válido hacia nodo aislado N10 (CA-SIM-05)")
    void shouldDiscardUnreachableUnitsWhenIncidentAtIsolatedN10() {
        Emergency emergency = emergencyAt("N10", EquipmentType.BASIC, SimulatedPriority.HIGH);
        List<Ambulance> catalog = List.of(
                Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"))
        );

        List<EligibleAmbulanceCandidate> result =
                policy.evaluateEligibleCandidates(emergency, catalog, sampleGraph());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Verifica pureza de dominio sin anotaciones de Spring, JPA ni Hibernate")
    void shouldKeepDispatchDomainPure() {
        Annotation[] annotations = AmbulanceEligibilityPolicy.class.getAnnotations();
        assertThat(Arrays.stream(annotations)
                .map(a -> a.annotationType().getPackageName()))
                .noneMatch(pkg -> pkg.startsWith("jakarta.persistence")
                        || pkg.startsWith("org.springframework")
                        || pkg.startsWith("org.hibernate"));
    }
}
