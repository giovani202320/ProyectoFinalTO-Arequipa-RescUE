package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.AmbulanceCatalogValidationException;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.presentation.AmbulanceCatalogController;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.TriageRuleCatalogApplicationService;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleEntry;
import com.arequiparescue.emergency.domain.TriageRuleStatus;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.emergency.presentation.TriageRuleCatalogController;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalCatalogValidationException;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.presentation.HospitalCatalogController;
import com.arequiparescue.routing.domain.GraphEdge;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.routing.presentation.RoadGraphTopologyController;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TriageRuleCatalogController.class,
        AmbulanceCatalogController.class,
        HospitalCatalogController.class,
        RoadGraphTopologyController.class
})
@Import(AreUiFormatter.class)
@DisplayName("Controladores MVC — Catálogos HU-03 y Versionado/Solo lectura HU-04")
class AdminAndRulesControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageRuleCatalogApplicationService triageRuleCatalogService;

    @MockitoBean
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @MockitoBean
    private HospitalCatalogApplicationService hospitalCatalogService;

    @MockitoBean
    private RoadGraphRepository roadGraphRepository;

    @MockitoBean
    private ActiveScenarioChecker activeScenarioChecker;

    private static TriageRuleVersion sampleRuleVersion(String id, int num, TriageRuleStatus status) {
        return new TriageRuleVersion(
                TriageRuleVersionId.of(id),
                num,
                status,
                LogicalTime.of(0),
                List.of(
                        new TriageRuleEntry(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, true),
                        new TriageRuleEntry(IncidentCategory.INC_B, SimulatedPriority.MEDIUM, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_C, SimulatedPriority.LOW, EquipmentType.BASIC, true),
                        new TriageRuleEntry(IncidentCategory.INC_D, SimulatedPriority.HIGH, EquipmentType.BASIC, true)
                )
        );
    }

    private static RoadGraph sampleGraphWithIsolatedN10() {
        return new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false),
                        new GraphNode(NodeId.of("N10"), "Z10", "Zona Aislada", true)
                ),
                List.of(
                        new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 5)
                )
        );
    }

    @Test
    @DisplayName("GET /triage-rules presenta solo lectura para ACT-01 y ACT-02 sin controles de edición/publicación (HU-04 CA-5)")
    void shouldServeReadOnlyRulesWithoutMutationControls() throws Exception {
        TriageRuleVersion v1 = sampleRuleVersion("RUL-V01", 1, TriageRuleStatus.ACTIVE);
        when(triageRuleCatalogService.findActiveVersion()).thenReturn(Optional.of(v1));
        when(triageRuleCatalogService.listAllVersions()).thenReturn(List.of(v1));
        when(triageRuleCatalogService.isAnyScenarioActive()).thenReturn(false);

        String readOnlyHtml = mockMvc.perform(get("/triage-rules").param("actor", "ACT-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Despachador (ACT-02)")))
                .andExpect(content().string(containsString("RUL-V01")))
                .andExpect(content().string(containsString("INC-A")))
                .andExpect(content().string(containsString("Activa")))
                .andExpect(content().string(containsString("Especializada")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(not(containsString("<form"))))
                .andExpect(content().string(not(containsString("type=\"submit\""))))
                .andExpect(content().string(not(containsString("Publicar y activar"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(readOnlyHtml, "triage-readonly-title");
    }

    @Test
    @DisplayName("GET y POST /admin/triage-rules publican nueva versión o devuelven HTTP 400 ante error")
    void shouldPublishRulesOrReturnBadRequestOnValidationFailure() throws Exception {
        TriageRuleVersion v1 = sampleRuleVersion("RUL-V01", 1, TriageRuleStatus.ACTIVE);
        TriageRuleVersion v2 = sampleRuleVersion("RUL-V02", 2, TriageRuleStatus.ACTIVE);
        when(triageRuleCatalogService.findActiveVersion()).thenReturn(Optional.of(v1));
        when(triageRuleCatalogService.listAllVersions()).thenReturn(List.of(v1));
        when(triageRuleCatalogService.isAnyScenarioActive()).thenReturn(false);

        String adminRulesHtml = mockMvc.perform(get("/admin/triage-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RUL-V01")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(adminRulesHtml, "triage-admin-title");
        assertThat(HtmlStructureAssertions.countOccurrences(adminRulesHtml, "<form ")).isEqualTo(1);

        when(triageRuleCatalogService.publishAndActivateNewVersion(any())).thenReturn(v2);

        mockMvc.perform(post("/admin/triage-rules")
                        .param("incAPriority", "HIGH")
                        .param("incAEquipment", "SPECIALIZED")
                        .param("incBPriority", "MEDIUM")
                        .param("incBEquipment", "BASIC")
                        .param("incCPriority", "LOW")
                        .param("incCEquipment", "BASIC")
                        .param("incDPriority", "HIGH")
                        .param("incDEquipment", "BASIC"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RUL-V02")));

        when(triageRuleCatalogService.publishAndActivateNewVersion(any()))
                .thenThrow(new EmergencyValidationException("global", "Operación rechazada durante escenario activo"));

        mockMvc.perform(post("/admin/triage-rules"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("escenario activo")));
    }

    @Test
    @DisplayName("GET y POST /admin/ambulances gestionan el catálogo y retornan HTTP 400 ante rechazo")
    void shouldManageAmbulanceCatalog() throws Exception {
        Ambulance amb = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.SPECIALIZED, NodeId.of("N01"));
        when(roadGraphRepository.loadGraph()).thenReturn(sampleGraphWithIsolatedN10());
        when(ambulanceCatalogService.listAmbulances()).thenReturn(List.of(amb));
        when(activeScenarioChecker.isAnyScenarioActive()).thenReturn(false);

        String ambulancesHtml = mockMvc.perform(get("/admin/ambulances"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("Especializada")))
                .andExpect(content().string(containsString("Disponible")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(ambulancesHtml, "ambulances-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(ambulancesHtml, "<form ")).isEqualTo(2);

        when(ambulanceCatalogService.registerAmbulance(any())).thenReturn(amb);

        mockMvc.perform(post("/admin/ambulances")
                        .param("id", "AMB-01")
                        .param("equipmentType", "SPECIALIZED")
                        .param("nodeId", "N01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("Especializada")));

        when(ambulanceCatalogService.registerAmbulance(any()))
                .thenThrow(new AmbulanceCatalogValidationException("global", "Escenario activo bloquea registro"));

        mockMvc.perform(post("/admin/ambulances")
                        .param("equipmentType", "BASIC")
                        .param("nodeId", "N01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Escenario activo")));
    }

    @Test
    @DisplayName("GET y POST /admin/hospitals gestionan capacidad C, R, O, D y rechazan violaciones con HTTP 400")
    void shouldManageHospitalCatalog() throws Exception {
        Hospital hospital = Hospital.registerInCatalog(
                HospitalId.of("HOSP-01"),
                "Hospital Base",
                NodeId.of("N01"),
                true,
                10
        );
        when(roadGraphRepository.loadGraph()).thenReturn(sampleGraphWithIsolatedN10());
        when(hospitalCatalogService.listHospitals()).thenReturn(List.of(hospital));
        when(activeScenarioChecker.isAnyScenarioActive()).thenReturn(false);

        String hospitalsHtml = mockMvc.perform(get("/admin/hospitals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HOSP-01")))
                .andExpect(content().string(containsString("Hospital Base")))
                .andExpect(content().string(containsString("Habilitado")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(hospitalsHtml, "hospitals-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(hospitalsHtml, "<form ")).isEqualTo(2);

        when(hospitalCatalogService.registerHospital(any())).thenReturn(hospital);

        mockMvc.perform(post("/admin/hospitals")
                        .param("id", "HOSP-01")
                        .param("name", "Hospital Base")
                        .param("nodeId", "N01")
                        .param("enabled", "true")
                        .param("totalCapacity", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals"));

        when(hospitalCatalogService.updateHospital(any()))
                .thenThrow(new HospitalCatalogValidationException("totalCapacity", "No se permite fijar C < R + O"));

        mockMvc.perform(post("/admin/hospitals/HOSP-01")
                        .param("name", "Hospital Base")
                        .param("nodeId", "N01")
                        .param("enabled", "true")
                        .param("totalCapacity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("C &lt; R + O")));
    }

    @Test
    @DisplayName("GET /admin/topology muestra nodos, costos positivos de aristas y condición aislada de N10 (HU-03 CA-4)")
    void shouldShowTopologyWithIsolatedN10() throws Exception {
        when(roadGraphRepository.loadGraph()).thenReturn(sampleGraphWithIsolatedN10());

        String html = mockMvc.perform(get("/admin/topology"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("N01")))
                .andExpect(content().string(containsString("N10")))
                .andExpect(content().string(containsString("EDG-01")))
                .andExpect(content().string(containsString("Conectado")))
                .andExpect(content().string(containsString("Nodo aislado sin aristas activas")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(html, "topology-page-title");
    }
}
