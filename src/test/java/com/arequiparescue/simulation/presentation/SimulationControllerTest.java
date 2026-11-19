package com.arequiparescue.simulation.presentation;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.application.SimulationStepOutcome;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.ScenarioStatus;
import com.arequiparescue.simulation.domain.SimulationScenario;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
@Import(AreUiFormatter.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulationScenarioApplicationService scenarioService;

    @MockitoBean
    private LogicalClock logicalClock;

    @Test
    void rendersSimulationPanelWithActiveScenarioAndPendingEvents() throws Exception {
        SimulationScenario scenario = new SimulationScenario("SCN-01", "Prueba", ScenarioStatus.ACTIVE, LogicalTime.of(3));
        ScheduledSimulationEvent nextEvent = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-01", LogicalTime.of(5), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "SYS", "Cola")
        );

        when(scenarioService.listScenarios()).thenReturn(List.of(scenario));
        when(scenarioService.findActiveScenario()).thenReturn(Optional.of(scenario));
        when(scenarioService.listScenarioEvents("SCN-01")).thenReturn(List.of(nextEvent));
        when(logicalClock.currentTicks()).thenReturn(3L);

        String html = mockMvc.perform(get("/simulation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Control de Escenarios y Avance Manual del Reloj Lógico")))
                .andExpect(content().string(containsString("SCN-01")))
                .andExpect(content().string(containsString("EV-01")))
                .andExpect(content().string(containsString("t=3")))
                .andExpect(content().string(containsString("Activo")))
                .andExpect(content().string(containsString("Evaluación de colas de espera")))
                .andExpect(content().string(containsString("Llegada de ambulancia al incidente")))
                .andExpect(content().string(containsString("Liberación de recurso")))
                .andExpect(content().string(containsString("Registro de emergencia")))
                .andExpect(content().string(containsString("Mínimo permitido: t &gt;= 3")))
                .andExpect(content().string(containsString("min=\"3\"")))
                .andExpect(content().string(containsString("SYS-QUEUE")))
                .andExpect(content().string(containsString("data-confirm-title=\"Finalizar escenario experimental\"")))
                .andExpect(content().string(containsString("id=\"areConfirmModal\"")))
                .andExpect(content().string(containsString("/js/are-ui.js")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(html, "simulation-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form")).isEqualTo(4);
    }

    @Test
    void createsScenarioAndRedirects() throws Exception {
        SimulationScenario created = SimulationScenario.startNew("SCN-01", "Escenario 01");
        when(scenarioService.createScenario("SCN-01", "Escenario 01")).thenReturn(created);

        mockMvc.perform(post("/simulation/scenarios")
                        .param("scenarioId", "SCN-01")
                        .param("name", "Escenario 01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void schedulesEventAndRedirects() throws Exception {
        ScheduledSimulationEvent scheduled = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-02", LogicalTime.of(7), DiscreteEventType.RESOURCE_RELEASE, 2L, "HOSP-01", "Cola hospital")
        );
        when(scenarioService.scheduleEvent(any())).thenReturn(scheduled);

        mockMvc.perform(post("/simulation/scenarios/SCN-01/events")
                        .param("eventId", "EV-02")
                        .param("scheduledAtTicks", "7")
                        .param("eventType", "RESOURCE_RELEASE")
                        .param("subjectId", "HOSP-01")
                        .param("description", "Cola hospital"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void advancesNextStepAndReportsWarnWhenNoPendingEvents() throws Exception {
        when(scenarioService.advanceNextStep("SCN-01")).thenReturn(new SimulationStepResult(
                "SCN-01",
                SimulationStepOutcome.NO_PENDING_EVENTS,
                LogicalTime.of(5),
                LogicalTime.of(5),
                null,
                null,
                "No hay eventos discretos pendientes por ejecutar."
        ));

        mockMvc.perform(post("/simulation/scenarios/SCN-01/advance"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("warnMessage"));
    }

    @Test
    void completesScenarioAndRedirects() throws Exception {
        SimulationScenario completed = new SimulationScenario("SCN-01", "Prueba", ScenarioStatus.COMPLETED, LogicalTime.of(10));
        when(scenarioService.completeScenario("SCN-01")).thenReturn(completed);

        mockMvc.perform(post("/simulation/scenarios/SCN-01/complete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void completedScenarioWithPendingEventsDoesNotShowNextExecutableEventAndRendersTraceability() throws Exception {
        SimulationScenario completed = new SimulationScenario("SCN-02", "Cerrado", ScenarioStatus.COMPLETED, LogicalTime.of(6));
        ScheduledSimulationEvent pendingEvent = ScheduledSimulationEvent.pending(
                "SCN-02",
                new DiscreteEvent("EV2-02", LogicalTime.of(7), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 2L, "AMB-02", "Pendiente al cierre")
        );

        when(scenarioService.listScenarios()).thenReturn(List.of(completed));
        when(scenarioService.findScenario("SCN-02")).thenReturn(Optional.of(completed));
        when(scenarioService.listScenarioEvents("SCN-02")).thenReturn(List.of(pendingEvent));
        when(logicalClock.currentTicks()).thenReturn(6L);

        mockMvc.perform(get("/simulation").param("scenarioId", "SCN-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Siguiente evento determinista a ejecutar"))))
                .andExpect(content().string(containsString("Escenario finalizado (COMPLETED): no admite nuevos eventos ni avance manual del reloj lógico.")))
                .andExpect(content().string(containsString("Eventos pendientes al cierre (solo trazabilidad, no ejecutables)")))
                .andExpect(content().string(containsString("EV2-02")));
    }

    @Test
    void advancesNextStepDistinguishesExecutedAndFailedOutcomes() throws Exception {
        ScheduledSimulationEvent executedEvent = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-OK", LogicalTime.of(2), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "SYS", "Cola")
        );
        executedEvent.markExecuted(LogicalTime.of(2), "Evento EV-OK ejecutado en t=2.");

        when(scenarioService.advanceNextStep("SCN-01")).thenReturn(new SimulationStepResult(
                "SCN-01",
                SimulationStepOutcome.EXECUTED,
                LogicalTime.of(0),
                LogicalTime.of(2),
                executedEvent,
                null,
                "Evento EV-OK ejecutado en t=2."
        ));

        mockMvc.perform(post("/simulation/scenarios/SCN-01/advance"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("successMessage"));

        ScheduledSimulationEvent failedEvent = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-BAD", LogicalTime.of(3), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 2L, "HOSP", "Inválido")
        );
        failedEvent.markFailed(LogicalTime.of(3), "Error al ejecutar evento EV-BAD en t=3: formato EMG-XXXXX.");

        when(scenarioService.advanceNextStep("SCN-01")).thenReturn(new SimulationStepResult(
                "SCN-01",
                SimulationStepOutcome.FAILED,
                LogicalTime.of(2),
                LogicalTime.of(3),
                failedEvent,
                null,
                "Error al ejecutar evento EV-BAD en t=3: formato EMG-XXXXX."
        ));

        mockMvc.perform(post("/simulation/scenarios/SCN-01/advance"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/simulation?scenarioId=SCN-01"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void rendersExecutedAndFailedHistoryWithDistinctStatusBadges() throws Exception {
        SimulationScenario scenario = new SimulationScenario("SCN-01", "Historial", ScenarioStatus.ACTIVE, LogicalTime.of(3));
        ScheduledSimulationEvent executedEvent = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-OK", LogicalTime.of(2), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "SYS", "Cola")
        );
        executedEvent.markExecuted(LogicalTime.of(2), "Evento EV-OK ejecutado en t=2.");

        ScheduledSimulationEvent failedEvent = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EV-BAD", LogicalTime.of(3), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 2L, "HOSP", "Inválido")
        );
        failedEvent.markFailed(LogicalTime.of(3), "Error al ejecutar evento EV-BAD en t=3: formato EMG-XXXXX.");

        when(scenarioService.listScenarios()).thenReturn(List.of(scenario));
        when(scenarioService.findActiveScenario()).thenReturn(Optional.of(scenario));
        when(scenarioService.listScenarioEvents("SCN-01")).thenReturn(List.of(executedEvent, failedEvent));
        when(logicalClock.currentTicks()).thenReturn(3L);

        mockMvc.perform(get("/simulation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("are-badge-success")))
                .andExpect(content().string(containsString("are-badge-danger")))
                .andExpect(content().string(containsString("Ejecutado")))
                .andExpect(content().string(containsString("Fallido")))
                .andExpect(content().string(containsString("EV-OK")))
                .andExpect(content().string(containsString("EV-BAD")));
    }
}
