package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.EmergencyNotFoundException;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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

    @BeforeEach
    void setUp() {
        when(emergencyApplicationService.currentLogicalTime()).thenReturn(LogicalTime.ZERO);
        when(emergencyApplicationService.findActiveRuleVersion())
                .thenReturn(Optional.of(TriageRuleVersion.initialApprovedCatalog()));
        when(emergencyApplicationService.listConfiguredGraphNodes()).thenReturn(List.of(
                new GraphNode(NodeId.of("N01"), "Z01", "N01 — Cercado Centro", false),
                new GraphNode(NodeId.of("N02"), "Z02", "N02 — Yanahuara", false),
                new GraphNode(NodeId.of("N10"), "Z10", "N10 — Yarabamba Rural", true)
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

        mockMvc.perform(post("/emergencies")
                        .param("incidentCategory", "INC-A")
                        .param("nodeId", "N01")
                        .param("description", "Emergencia simulada en Cercado"))
                .andExpect(status().isOk())
                .andExpect(view().name("emergencies/detail"))
                .andExpect(model().attribute("justRegistered", true))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("INC-A")))
                .andExpect(content().string(containsString("N01")))
                .andExpect(content().string(containsString("Alta")))
                .andExpect(content().string(containsString("Especializada")))
                .andExpect(content().string(containsString("RUL-V01")))
                .andExpect(content().string(containsString("Registrada")))
                .andExpect(content().string(containsString("/css/are-design-system.css")));
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

        mockMvc.perform(get("/emergencies/EMG-99999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("emergencies/list"))
                .andExpect(content().string(containsString("EMG-99999")));
    }
}
