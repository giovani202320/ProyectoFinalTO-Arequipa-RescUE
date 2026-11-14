package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WaitingQueueController.class)
@DisplayName("WaitingQueueController — Vista y evaluación FIFO de WAITING_AMBULANCE (HU-08)")
class WaitingQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchApplicationService dispatchApplicationService;

    @Test
    @DisplayName("GET /dispatch/waiting-queue muestra cola FIFO, unidades AVAILABLE y avisos de agotamiento")
    void shouldShowWaitingQueueAndExhaustedNotices() throws Exception {
        Emergency waiting = Emergency.register(
                EmergencyId.of("EMG-00001"),
                NodeId.of("N02"),
                "Emergencia en espera",
                new TriageClassification(IncidentCategory.INC_C, SimulatedPriority.MEDIUM,
                        EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01")),
                LogicalTime.of(1L)
        ).enterOrRemainInWaitingAmbulance(LogicalTime.of(2L), "CONCURRENCY_RETRY_EXHAUSTED");

        Ambulance amb = Ambulance.registerInCatalog(
                AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"));

        when(dispatchApplicationService.listWaitingAmbulanceQueue()).thenReturn(List.of(waiting));
        when(dispatchApplicationService.listAvailableAmbulances()).thenReturn(List.of(amb));
        when(dispatchApplicationService.listRecentExhaustedNotices()).thenReturn(List.of(
                new DispatchTraceabilityRepository.ExhaustedDispatchNotice(
                        "AUTO-1", "AUTOMATIC_DISPATCH", "EMG-00001", 4,
                        "CONCURRENCY_RETRY_EXHAUSTED", "Agotamiento tras 4 intentos", 2L
                )
        ));

        mockMvc.perform(get("/dispatch/waiting-queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("dispatch/waiting-queue"))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("CONCURRENCY_RETRY_EXHAUSTED")));
    }

    @Test
    @DisplayName("POST /dispatch/waiting-queue/evaluate ejecuta evaluación FIFO y muestra el resumen")
    void shouldEvaluateWaitingQueueAndRenderSummary() throws Exception {
        when(dispatchApplicationService.evaluateWaitingAmbulanceQueue()).thenReturn(
                new WaitingQueueEvaluationResult(1, 1, 0, List.of(), "1 asignación confirmada en orden FIFO.")
        );
        when(dispatchApplicationService.listWaitingAmbulanceQueue()).thenReturn(List.of());
        when(dispatchApplicationService.listAvailableAmbulances()).thenReturn(List.of());
        when(dispatchApplicationService.listRecentExhaustedNotices()).thenReturn(List.of());

        mockMvc.perform(post("/dispatch/waiting-queue/evaluate"))
                .andExpect(status().isOk())
                .andExpect(view().name("dispatch/waiting-queue"))
                .andExpect(content().string(containsString("1 asignación confirmada en orden FIFO.")));
    }
}
