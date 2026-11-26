package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.EmergencyNotFoundException;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.RouteSegmentType;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EmergencyController.class)
@Import(AreUiFormatter.class)
@DisplayName("EmergencyController — Registro HTTP y consulta Thymeleaf (HU-01, HU-02)")
class EmergencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmergencyApplicationService emergencyApplicationService;

    @MockitoBean
    private HospitalCatalogApplicationService hospitalCatalogApplicationService;

    @MockitoBean
    private RoadGraphRepository roadGraphRepository;

    @BeforeEach
    void setUp() {
        when(emergencyApplicationService.currentLogicalTime()).thenReturn(LogicalTime.ZERO);
        when(emergencyApplicationService.findActiveRuleVersion())
                .thenReturn(Optional.of(TriageRuleVersion.initialApprovedCatalog()));
        List<GraphNode> nodes = List.of(
                new GraphNode(NodeId.of("N01"), "Z01", "N01 — Cercado Centro", false),
                new GraphNode(NodeId.of("N02"), "Z02", "N02 — Yanahuara", false),
                new GraphNode(NodeId.of("N10"), "Z10", "N10 — Yarabamba Rural", true)
        );
        when(emergencyApplicationService.listConfiguredGraphNodes()).thenReturn(nodes);
        when(roadGraphRepository.loadGraph()).thenReturn(new RoadGraph(
                nodes,
                List.of(new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 5))
        ));
        when(hospitalCatalogApplicationService.listHospitals()).thenReturn(List.of(
                Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "Hospital Base", NodeId.of("N02"), true, 10)
        ));
    }

    @Test
    @DisplayName("GET /emergencies/new renderiza el formulario Thymeleaf con los 3 campos aprobados (HU-01)")
    void shouldRenderRegistrationForm() throws Exception {
        String html = mockMvc.perform(get("/emergencies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("emergencies/form"))
                .andExpect(content().string(containsString("Registro y clasificación simulada de emergencia")))
                .andExpect(content().string(containsString("INC-A")))
                .andExpect(content().string(containsString("N01 — Cercado Centro")))
                .andExpect(content().string(containsString("RUL-V01")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        HtmlStructureAssertions.assertSingleAppShellStructure(html, "emergency-form-title");
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form ")).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /emergencies registra una emergencia válida y muestra identificador, categoría, clasificación y estado REGISTERED (HU-01, HU-02)")
    void shouldRegisterEmergencyViaHttpAndDisplayClassificationAndState() throws Exception {
        Emergency registered = Emergency.register(
                EmergencyId.of("EMG-00001"),
                NodeId.of("N01"),
                "Emergencia simulada en Cercado",
                new TriageClassification(
                        IncidentCategory.INC_A,
                        SimulatedPriority.HIGH,
                        EquipmentType.SPECIALIZED,
                        TriageRuleVersionId.of("RUL-V01")
                ),
                LogicalTime.ZERO
        );
        when(emergencyApplicationService.registerEmergency(any(RegisterEmergencyCommand.class)))
                .thenReturn(registered);
        when(emergencyApplicationService.getEmergencyById("EMG-00001"))
                .thenReturn(registered);

        mockMvc.perform(post("/emergencies")
                        .param("incidentCategory", "INC-A")
                        .param("nodeId", "N01")
                        .param("description", "Emergencia simulada en Cercado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/emergencies/EMG-00001"))
                .andExpect(flash().attribute("justRegistered", true));

        mockMvc.perform(get("/emergencies/EMG-00001").flashAttr("justRegistered", true))
                .andExpect(status().isOk())
                .andExpect(view().name("emergencies/detail"))
                .andExpect(model().attribute("justRegistered", true))
                .andExpect(content().string(containsString("Emergencia registrada y clasificada correctamente")))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("INC-A")))
                .andExpect(content().string(containsString("N01")))
                .andExpect(content().string(containsString("Alta")))
                .andExpect(content().string(containsString("Especializada")))
                .andExpect(content().string(containsString("RUL-V01")))
                .andExpect(content().string(containsString("Registrada")))
                .andExpect(content().string(containsString("/css/are-design-system.css")));

        mockMvc.perform(get("/emergencies/EMG-00001"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("justRegistered", false))
                .andExpect(content().string(not(containsString("Emergencia registrada y clasificada correctamente"))));

        verify(emergencyApplicationService, times(1)).registerEmergency(any(RegisterEmergencyCommand.class));
    }

    @Test
    @DisplayName("POST /emergencies rechaza solicitudes sin categoría o sin nodo devolviendo HTTP 400 y mensajes claros sin persistir (HU-01 CA-2)")
    void shouldRejectBlankFieldsWithHttp400AndClearErrorMessages() throws Exception {
        mockMvc.perform(post("/emergencies")
                        .param("incidentCategory", "")
                        .param("nodeId", "")
                        .param("description", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("emergencies/form"))
                .andExpect(model().attributeHasFieldErrors("form", "incidentCategory", "nodeId"))
                .andExpect(content().string(containsString("La categoría del incidente es obligatoria")))
                .andExpect(content().string(containsString("El nodo de ubicación en el grafo es obligatorio")));

        verify(emergencyApplicationService, never()).registerEmergency(any());
    }

    @Test
    @DisplayName("POST /emergencies rechaza categoría inexistente o nodo fuera de N01-N10 con HTTP 400 y mensaje comprensible")
    void shouldRejectInvalidDomainCategoryOrNodeWithHttp400() throws Exception {
        when(emergencyApplicationService.registerEmergency(any(RegisterEmergencyCommand.class)))
                .thenThrow(new EmergencyValidationException(
                        "nodeId",
                        "El nodo de ubicación 'N99' no es válido. Debe pertenecer al catálogo N01 a N10 del grafo configurado (RN-EME-010.3)."
                ));

        mockMvc.perform(post("/emergencies")
                        .param("incidentCategory", "INC-B")
                        .param("nodeId", "N99")
                        .param("description", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("emergencies/form"))
                .andExpect(model().attributeHasFieldErrors("form", "nodeId"))
                .andExpect(content().string(containsString("N01 a N10")));
    }

    @Test
    @DisplayName("GET /emergencies y GET /emergencies/{id} muestran el listado y detalle trazable de emergencias (HU-02)")
    void shouldRenderEmergencyListAndDetailViews() throws Exception {
        Emergency sample = Emergency.reconstruct(
                EmergencyId.of("EMG-00001"),
                IncidentCategory.INC_C,
                NodeId.of("N02"),
                "",
                SimulatedPriority.LOW,
                EquipmentType.BASIC,
                TriageRuleVersionId.of("RUL-V01"),
                EmergencyState.REGISTERED,
                null,
                null,
                LogicalTime.of(3L),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(emergencyApplicationService.listEmergencies()).thenReturn(List.of(sample));
        when(emergencyApplicationService.getEmergencyById("EMG-00001")).thenReturn(sample);
        when(emergencyApplicationService.getEmergencyById("EMG-99999"))
                .thenThrow(new EmergencyNotFoundException("EMG-99999"));

        String listHtml = mockMvc.perform(get("/emergencies"))
                .andExpect(status().isOk())
                .andExpect(view().name("emergencies/list"))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("INC-C")))
                .andExpect(content().string(containsString("Baja")))
                .andExpect(content().string(containsString("Básica")))
                .andExpect(content().string(containsString("Registrada")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(listHtml, "emergencies-page-title");

        String detailHtml = mockMvc.perform(get("/emergencies/EMG-00001"))
                .andExpect(status().isOk())
                .andExpect(view().name("emergencies/detail"))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("Sin descripción ficticia (vacía)")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(detailHtml, "detail-header-title");
        assertThat(HtmlStructureAssertions.countOccurrences(detailHtml, "Detalle de emergencia")).isEqualTo(1);
        assertThat(detailHtml)
                .contains("Recorrido de atención de emergencia")
                .contains("Sin ambulancia asignada. No existe recorrido histórico.")
                .contains("Destino hospitalario aún no definido.")
                .contains("id=\"areTopologySvg\"")
                .contains("/js/are-topology-graph.js?v=5a-a07");

        Emergency assignedNoHospital = Emergency.register(
                        EmergencyId.of("EMG-00003"),
                        NodeId.of("N01"),
                        "Asignada sin hospital",
                        new TriageClassification(IncidentCategory.INC_B, SimulatedPriority.MEDIUM, EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01")),
                        LogicalTime.ZERO)
                .assignAmbulance("AMB-01", LogicalTime.of(1));
        when(emergencyApplicationService.getEmergencyById("EMG-00003")).thenReturn(assignedNoHospital);

        mockMvc.perform(get("/emergencies/EMG-00003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("Origen histórico, secuencia y costo del Tramo A no disponibles")))
                .andExpect(content().string(containsString("Destino hospitalario aún no definido.")));

        Emergency inTransport = Emergency.register(
                        EmergencyId.of("EMG-00002"),
                        NodeId.of("N01"),
                        "Traslado",
                        new TriageClassification(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01")),
                        LogicalTime.ZERO)
                .assignAmbulance("AMB-01", LogicalTime.of(1))
                .arriveAtIncident(LogicalTime.of(2))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(3));
        EmergencyRouteSegment segmentA = new EmergencyRouteSegment(
                EmergencyId.of("EMG-00002"),
                RouteSegmentType.AMBULANCE_TO_INCIDENT,
                "AMB-01",
                NodeId.of("N02"),
                NodeId.of("N01"),
                List.of(NodeId.of("N02"), NodeId.of("N01")),
                5,
                1L);
        EmergencyRouteSegment segmentB = new EmergencyRouteSegment(
                EmergencyId.of("EMG-00002"),
                RouteSegmentType.INCIDENT_TO_HOSPITAL,
                "HOSP-01",
                NodeId.of("N01"),
                NodeId.of("N02"),
                List.of(NodeId.of("N01"), NodeId.of("N02")),
                5,
                3L);
        when(emergencyApplicationService.getEmergencyById("EMG-00002")).thenReturn(inTransport);
        when(emergencyApplicationService.findRouteSegments(EmergencyId.of("EMG-00002")))
                .thenReturn(List.of(segmentA, segmentB));

        String inTransportHtml = mockMvc.perform(get("/emergencies/EMG-00002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recorrido de atención de emergencia")))
                .andExpect(content().string(containsString("HOSP-01")))
                .andExpect(content().string(containsString("Hospital Base")))
                .andExpect(content().string(containsString("N02 → N01")))
                .andExpect(content().string(containsString("N01 → N02")))
                .andExpect(content().string(containsString("id=\"areTopologySvg\"")))
                .andExpect(content().string(containsString("routePathNodesA: [\"N02\",\"N01\"]")))
                .andExpect(content().string(containsString("routePathNodesB: [\"N01\",\"N02\"]")))
                .andExpect(content().string(containsString("10 unidades abstractas")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(inTransportHtml, "detail-header-title");

        Emergency completed = inTransport.completeHospitalDelivery(LogicalTime.of(5));
        when(emergencyApplicationService.getEmergencyById("EMG-00004")).thenReturn(completed);
        when(emergencyApplicationService.findRouteSegments(EmergencyId.of("EMG-00004")))
                .thenReturn(List.of(
                        new EmergencyRouteSegment(
                                EmergencyId.of("EMG-00004"),
                                RouteSegmentType.AMBULANCE_TO_INCIDENT,
                                "AMB-01",
                                NodeId.of("N02"),
                                NodeId.of("N01"),
                                List.of(NodeId.of("N02"), NodeId.of("N01")),
                                5,
                                1L),
                        new EmergencyRouteSegment(
                                EmergencyId.of("EMG-00004"),
                                RouteSegmentType.INCIDENT_TO_HOSPITAL,
                                "HOSP-01",
                                NodeId.of("N01"),
                                NodeId.of("N02"),
                                List.of(NodeId.of("N01"), NodeId.of("N02")),
                                5,
                                3L)));
        mockMvc.perform(get("/emergencies/EMG-00004"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("routePathNodesA: [\"N02\",\"N01\"]")))
                .andExpect(content().string(containsString("routePathNodesB: [\"N01\",\"N02\"]")))
                .andExpect(content().string(containsString("10 unidades abstractas")));

        Emergency historicalWithoutSegments = Emergency.register(
                        EmergencyId.of("EMG-00005"),
                        NodeId.of("N10"),
                        "Histórica previa a V8",
                        new TriageClassification(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01")),
                        LogicalTime.ZERO)
                .assignAmbulance("AMB-01", LogicalTime.of(1))
                .arriveAtIncident(LogicalTime.of(2))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(3));
        when(emergencyApplicationService.getEmergencyById("EMG-00005")).thenReturn(historicalWithoutSegments);
        when(emergencyApplicationService.findRouteSegments(EmergencyId.of("EMG-00005"))).thenReturn(List.of());
        mockMvc.perform(get("/emergencies/EMG-00005"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Datos históricos de ruta no disponibles para el hospital HOSP-01")));

        mockMvc.perform(get("/emergencies/EMG-99999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("emergencies/list"))
                .andExpect(content().string(containsString("EMG-99999")));
    }

    @Test
    @DisplayName("UX-04 y UX-03: GET /emergencies filtra por ID y estado y existe plantilla error.html segura")
    void shouldFilterEmergenciesByQueryAndStateAndProvideSafeErrorTemplate() throws Exception {
        Emergency e1 = Emergency.register(
                EmergencyId.of("EMG-00001"), NodeId.of("N01"), "Primera",
                new TriageClassification(IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01")),
                LogicalTime.ZERO);
        Emergency e2 = Emergency.register(
                EmergencyId.of("EMG-00002"), NodeId.of("N02"), "Segunda",
                new TriageClassification(IncidentCategory.INC_C, SimulatedPriority.LOW, EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01")),
                LogicalTime.of(2)).enterOrRemainInWaitingAmbulance(LogicalTime.of(2), "NO_ELIGIBLE_AMBULANCE");

        when(emergencyApplicationService.listEmergencies()).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/emergencies").param("q", "EMG-00002").param("state", "WAITING_AMBULANCE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EMG-00002")))
                .andExpect(content().string(not(containsString(">EMG-00001<"))))
                .andExpect(content().string(containsString("1 / 2")));

        mockMvc.perform(get("/emergencies").param("q", "EMG-99999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay emergencias que coincidan con el filtro")));

        String errorTemplate = new String(
                new ClassPathResource("templates/error.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(errorTemplate)
                .contains("fragments/layout :: appShell")
                .doesNotContain("th:utext")
                .doesNotContain("${trace}");
    }
}
