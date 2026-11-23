package com.arequiparescue.simulation.presentation;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.simulation.application.ScheduleSimulationEventCommand;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.application.SimulationStepOutcome;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.DiscreteEventStatus;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.SimulationScenario;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controlador MVC para la administración de escenarios experimentales y avance manual paso a paso
 * del reloj lógico mediante eventos discretos (HU-13, ACT-03).
 */
@Controller
@RequestMapping("/simulation")
public class SimulationController {

    private final SimulationScenarioApplicationService scenarioService;
    private final LogicalClock logicalClock;
    private final SimulationPanelViewHelper panelViewHelper;

    public SimulationController(
            SimulationScenarioApplicationService scenarioService,
            LogicalClock logicalClock,
            SimulationPanelViewHelper panelViewHelper
    ) {
        this.scenarioService = Objects.requireNonNull(scenarioService);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.panelViewHelper = Objects.requireNonNull(panelViewHelper);
    }

    @GetMapping
    public String panel(
            @RequestParam(name = "scenarioId", required = false) String scenarioId,
            Model model
    ) {
        List<SimulationScenario> scenarios = scenarioService.listScenarios();
        Optional<SimulationScenario> selectedScenario = resolveSelectedScenario(scenarioId, scenarios);
        SimulationPanelViewHelper.OperationalSnapshot operationalSnapshot = panelViewHelper.buildOperationalSnapshot();

        model.addAttribute("scenarios", scenarios);
        model.addAttribute("selectedScenario", selectedScenario.orElse(null));
        model.addAttribute("eventTypes", DiscreteEventType.values());
        model.addAttribute("globalClockTicks", logicalClock.currentTicks());
        model.addAttribute("operationalSnapshot", operationalSnapshot);

        if (selectedScenario.isPresent()) {
            String activeId = selectedScenario.get().id();
            List<ScheduledSimulationEvent> allEvents = scenarioService.listScenarioEvents(activeId);
            List<ScheduledSimulationEvent> pendingEvents = allEvents.stream()
                    .filter(ScheduledSimulationEvent::isPending)
                    .toList();
            List<ScheduledSimulationEvent> historyEvents = allEvents.stream()
                    .filter(event -> event.status() != DiscreteEventStatus.PENDING)
                    .toList();
            model.addAttribute("pendingEvents", pendingEvents);
            model.addAttribute("historyEvents", historyEvents);
            boolean isActive = selectedScenario.get().isActive();
            model.addAttribute("nextPendingEvent", isActive && !pendingEvents.isEmpty() ? pendingEvents.getFirst() : null);
            model.addAttribute("latestHistoryEvent", !historyEvents.isEmpty() ? historyEvents.getLast() : null);
        } else {
            model.addAttribute("pendingEvents", List.of());
            model.addAttribute("historyEvents", List.of());
            model.addAttribute("nextPendingEvent", null);
            model.addAttribute("latestHistoryEvent", null);
        }

        return "simulation/scenarios";
    }

    @PostMapping("/scenarios")
    public String createScenario(
            @RequestParam(name = "scenarioId", required = false) String scenarioId,
            @RequestParam(name = "name", required = false) String name,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SimulationScenario created = scenarioService.createScenario(scenarioId, name);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Escenario experimental " + created.id() + " iniciado en estado ACTIVE (t=0)."
            );
            return "redirect:/simulation?scenarioId=" + created.id();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/simulation";
        }
    }

    @PostMapping("/scenarios/{scenarioId}/events")
    public String scheduleEvent(
            @PathVariable("scenarioId") String scenarioId,
            @RequestParam(name = "eventId", required = false) String eventId,
            @RequestParam(name = "scheduledAtTicks", required = false) Long scheduledAtTicks,
            @RequestParam(name = "eventType", required = false) DiscreteEventType eventType,
            @RequestParam(name = "subjectId", required = false) String subjectId,
            @RequestParam(name = "description", required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (scheduledAtTicks == null) {
                throw new IllegalArgumentException("El tiempo lógico programado (t) es obligatorio.");
            }
            if (eventType == null) {
                throw new IllegalArgumentException("El tipo de evento discreto es obligatorio.");
            }
            ScheduledSimulationEvent scheduled = scenarioService.scheduleEvent(new ScheduleSimulationEventCommand(
                    scenarioId,
                    eventId,
                    scheduledAtTicks,
                    eventType,
                    subjectId,
                    description
            ));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Evento discreto " + scheduled.event().eventId() + " programado en t="
                            + scheduled.event().scheduledAt().ticks()
                            + " (secuencia #" + scheduled.event().sequenceNumber() + ")."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/simulation?scenarioId=" + scenarioId;
    }

    @PostMapping("/scenarios/{scenarioId}/advance")
    public String advanceNextStep(
            @PathVariable("scenarioId") String scenarioId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SimulationPanelViewHelper.RawOperationalState before = panelViewHelper.captureRawState();
            SimulationStepResult result = scenarioService.advanceNextStep(scenarioId);
            if (result.outcome() == SimulationStepOutcome.EXECUTED) {
                SimulationPanelViewHelper.RawOperationalState after = panelViewHelper.captureRawState();
                redirectAttributes.addFlashAttribute(
                        "lastStepComparison",
                        SimulationPanelViewHelper.buildStepComparison(before, after, result));
                redirectAttributes.addFlashAttribute("successMessage", result.message());
            } else if (result.outcome() == SimulationStepOutcome.NO_PENDING_EVENTS) {
                redirectAttributes.addFlashAttribute("warnMessage", result.message());
            } else {
                SimulationPanelViewHelper.RawOperationalState after = panelViewHelper.captureRawState();
                redirectAttributes.addFlashAttribute(
                        "lastStepComparison",
                        SimulationPanelViewHelper.buildStepComparison(before, after, result));
                redirectAttributes.addFlashAttribute("errorMessage", result.message());
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/simulation?scenarioId=" + scenarioId;
    }

    @PostMapping("/scenarios/{scenarioId}/complete")
    public String completeScenario(
            @PathVariable("scenarioId") String scenarioId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SimulationScenario completed = scenarioService.completeScenario(scenarioId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Escenario " + completed.id() + " finalizado en estado COMPLETED (t="
                            + completed.currentTime().ticks() + ")."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/simulation?scenarioId=" + scenarioId;
    }

    private Optional<SimulationScenario> resolveSelectedScenario(
            String requestedId,
            List<SimulationScenario> scenarios
    ) {
        if (requestedId != null && !requestedId.isBlank()) {
            return scenarioService.findScenario(requestedId.trim());
        }
        Optional<SimulationScenario> active = scenarioService.findActiveScenario();
        if (active.isPresent()) {
            return active;
        }
        return scenarios.isEmpty() ? Optional.empty() : Optional.of(scenarios.getFirst());
    }
}
