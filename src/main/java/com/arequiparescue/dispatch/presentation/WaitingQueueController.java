package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.DispatchValidationException;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.emergency.presentation.EmergencyView;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

/**
 * Controlador Spring MVC para la consulta y atención FIFO de la cola {@code WAITING_AMBULANCE} (HU-08)
 * y visualización de avisos de agotamiento de reintentos (HU-07) por el Despachador ({@code ACT-02}).
 */
@Controller
@RequestMapping("/dispatch/waiting-queue")
public class WaitingQueueController {

    private final DispatchApplicationService dispatchApplicationService;

    public WaitingQueueController(DispatchApplicationService dispatchApplicationService) {
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
    }

    @GetMapping
    public String showWaitingQueue(Model model) {
        populateQueueModel(model);
        return "dispatch/waiting-queue";
    }

    @PostMapping("/evaluate")
    public String evaluateWaitingQueue(
            @RequestParam(name = "ambulanceId", required = false) String ambulanceId,
            Model model,
            HttpServletResponse response
    ) {
        try {
            WaitingQueueEvaluationResult evaluation = (ambulanceId != null && !ambulanceId.isBlank())
                    ? dispatchApplicationService.evaluateWaitingAmbulanceQueueForAmbulance(ambulanceId)
                    : dispatchApplicationService.evaluateWaitingAmbulanceQueue();
            model.addAttribute("evaluationResult", evaluation);
        } catch (DispatchValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        populateQueueModel(model);
        return "dispatch/waiting-queue";
    }

    private void populateQueueModel(Model model) {
        List<EmergencyView> waitingQueue = dispatchApplicationService.listWaitingAmbulanceQueue().stream()
                .map(EmergencyView::fromDomain)
                .toList();
        List<Ambulance> availableAmbulances = dispatchApplicationService.listAvailableAmbulances();
        model.addAttribute("waitingQueue", waitingQueue);
        model.addAttribute("availableAmbulances", availableAmbulances);
        model.addAttribute("exhaustedNotices", dispatchApplicationService.listRecentExhaustedNotices());
    }
}
