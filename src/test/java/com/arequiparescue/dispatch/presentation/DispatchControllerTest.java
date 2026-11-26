package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.dispatch.application.AutomaticDispatchResult;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.SupervisedDispatchResult;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
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
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DispatchController.class)
@Import(AreUiFormatter.class)
@DisplayName("Controlador MVC — Despacho supervisado y elegibilidad (HU-05 y HU-06)")
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchApplicationService dispatchApplicationService;

    @MockitoBean
    private RoadGraphRepository roadGraphRepository;

    @BeforeEach
    void setUpGraph() {
        when(roadGraphRepository.loadGraph()).thenReturn(new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false)
                ),
                List.of(new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 5))
        ));
    }

    private static Emergency sampleRegisteredEmergency() {
        return Emergency.register(
                EmergencyId.of("EMG-00001"),
                NodeId.of("N01"),
                "Incidente de prueba",
                new TriageClassification(
                        IncidentCategory.INC_B,
                        SimulatedPriority.MEDIUM,
                        EquipmentType.BASIC,
                        TriageRuleVersionId.of("RUL-V01")
                ),
                LogicalTime.of(0)
        );
    }

    @Test
    @DisplayName("GET /dispatch/emergencies/{id} lista ambulancias elegibles ordenadas para selección libre (HU-05)")
    void shouldRenderEligibleAmbulancesForSupervisedDispatch() throws Exception {
        Emergency emergency = sampleRegisteredEmergency();
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Ambulance amb2 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.BASIC, NodeId.of("N02"));

        when(roadGraphRepository.loadGraph()).thenReturn(new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false)
                ),
                List.of(new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 5))
        ));

        when(dispatchApplicationService.evaluateEligibleAmbulances("EMG-00001"))
                .thenReturn(new DispatchApplicationService.EligibleEvaluation(
                        emergency,
                        List.of(
                                new EligibleAmbulanceCandidate(amb1, 0, List.of(NodeId.of("N01")), false),
                                new EligibleAmbulanceCandidate(amb2, 5, List.of(NodeId.of("N02"), NodeId.of("N01")), false)
                        )
                ));

        String html = mockMvc.perform(get("/dispatch/emergencies/EMG-00001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("AMB-02")))
                .andExpect(content().string(containsString("Recomendada")))
                .andExpect(content().string(containsString("Registrada")))
                .andExpect(content().string(containsString("Básica")))
                .andExpect(content().string(containsString("Disponible")))
                .andExpect(content().string(containsString("id=\"supervisedDispatchGraphCard\"")))
                .andExpect(content().string(containsString("id=\"areTopologySvg\"")))
                .andExpect(content().string(containsString("data-preview-amb-id=\"AMB-01\"")))
                .andExpect(content().string(containsString("data-preview-amb-id=\"AMB-02\"")))
                .andExpect(content().string(containsString("data-preview-nodes=\"N02,N01\"")))
                .andExpect(content().string(containsString("/js/are-topology-graph.js?v=5a-fix")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(html, "supervised-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form")).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /dispatch/emergencies/{id}/assign redirige ante éxito y devuelve HTTP 409 con lista actualizada ante conflicto (HU-06)")
    void shouldHandleSupervisedAssignmentSuccessAndConflictReload() throws Exception {
        Emergency emergency = sampleRegisteredEmergency();
        Ambulance amb2 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-02"), EquipmentType.BASIC, NodeId.of("N02"));
        Emergency assigned = emergency.assignAmbulance("AMB-02", LogicalTime.of(0));

        when(dispatchApplicationService.assignSupervised("EMG-00001", "AMB-02"))
                .thenReturn(new SupervisedDispatchResult(
                        "SUP-1",
                        DispatchOperationOutcome.ASSIGNED,
                        assigned,
                        amb2.assignToEmergency(emergency.id()),
                        List.of(),
                        "Asignación supervisada confirmada"
                ));

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/assign").param("ambulanceId", "AMB-02"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/emergencies/EMG-00001"));

        when(dispatchApplicationService.assignSupervised("EMG-00001", "AMB-01"))
                .thenReturn(new SupervisedDispatchResult(
                        "SUP-2",
                        DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED,
                        emergency,
                        null,
                        List.of(new EligibleAmbulanceCandidate(amb2, 5, List.of(NodeId.of("N02"), NodeId.of("N01")), false)),
                        "AMB-01 fue tomada por otra operación concurrente"
                ));

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/assign").param("ambulanceId", "AMB-01"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Conflicto concurrente detectado")))
                .andExpect(content().string(containsString("AMB-02")));
    }

    @Test
    @DisplayName("POST /dispatch/emergencies/{id}/assign-automatic redirige ante éxito y muestra aviso HTTP 409 ante CONCURRENCY_RETRY_EXHAUSTED (HU-07)")
    void shouldHandleAutomaticDispatchSuccessWaitingAndExhaustion() throws Exception {
        Emergency emergency = sampleRegisteredEmergency();
        Ambulance amb1 = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));
        Emergency assigned = emergency.assignAmbulance("AMB-01", LogicalTime.of(0));

        when(dispatchApplicationService.assignAutomatic("EMG-00001"))
                .thenReturn(new AutomaticDispatchResult(
                        "AUTO-1",
                        DispatchOperationOutcome.ASSIGNED,
                        assigned,
                        amb1.assignToEmergency(emergency.id()),
                        1,
                        0,
                        "ASSIGNED",
                        "Asignación automática confirmada"
                ));

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/assign-automatic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/emergencies/EMG-00001"));

        Emergency waiting = emergency.enterOrRemainInWaitingAmbulance(
                LogicalTime.of(0), "CONCURRENCY_RETRY_EXHAUSTED");
        when(dispatchApplicationService.assignAutomatic("EMG-00001"))
                .thenReturn(new AutomaticDispatchResult(
                        "AUTO-2",
                        DispatchOperationOutcome.CONCURRENCY_RETRY_EXHAUSTED,
                        waiting,
                        null,
                        4,
                        3,
                        "CONCURRENCY_RETRY_EXHAUSTED",
                        "Agotamiento tras 4 intentos para EMG-00001"
                ));

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/assign-automatic"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("CONCURRENCY_RETRY_EXHAUSTED")))
                .andExpect(content().string(containsString("Agotamiento tras 4 intentos para EMG-00001")));
    }
}
