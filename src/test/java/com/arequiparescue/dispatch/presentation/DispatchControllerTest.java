package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
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
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DispatchController.class)
@DisplayName("Controlador MVC — Despacho supervisado y elegibilidad (HU-05 y HU-06)")
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchApplicationService dispatchApplicationService;

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

        when(dispatchApplicationService.evaluateEligibleAmbulances("EMG-00001"))
                .thenReturn(new DispatchApplicationService.EligibleEvaluation(
                        emergency,
                        List.of(
                                new EligibleAmbulanceCandidate(amb1, 0, List.of(NodeId.of("N01")), false),
                                new EligibleAmbulanceCandidate(amb2, 5, List.of(NodeId.of("N02"), NodeId.of("N01")), false)
                        )
                ));

        mockMvc.perform(get("/dispatch/emergencies/EMG-00001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("AMB-02")))
                .andExpect(content().string(containsString("Recomendada")));
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
}
