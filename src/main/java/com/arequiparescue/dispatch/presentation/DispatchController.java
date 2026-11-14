package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.DispatchValidationException;
import com.arequiparescue.dispatch.application.SupervisedDispatchResult;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.presentation.EmergencyView;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controlador Spring MVC para la consulta de ambulancias elegibles (HU-05) y la asignación
 * supervisada con revalidación concurrente (HU-06) por el Despachador ({@code ACT-02}).
 */
@Controller
@RequestMapping("/dispatch/emergencies")
public class DispatchController {

    private final DispatchApplicationService dispatchApplicationService;

    public DispatchController(DispatchApplicationService dispatchApplicationService) {
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
    }

    @GetMapping("/{id}")
    public String showSupervisedDispatchPanel(
            @PathVariable("id") String id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            DispatchApplicationService.EligibleEvaluation evaluation =
                    dispatchApplicationService.evaluateEligibleAmbulances(id);
            populatePanelModel(model, evaluation.emergency(), evaluation.candidates());
            return "dispatch/supervised";
        } catch (DispatchValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "dispatch/supervised";
        }
    }

    @PostMapping("/{id}/assign")
    public String assignSupervised(
            @PathVariable("id") String id,
            @RequestParam(name = "ambulanceId", required = false) String ambulanceId,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SupervisedDispatchResult result = dispatchApplicationService.assignSupervised(id, ambulanceId);
            if (result.outcome() == DispatchOperationOutcome.ASSIGNED) {
                redirectAttributes.addFlashAttribute("dispatchSuccessMessage", result.message());
                return "redirect:/emergencies/" + result.emergency().id().value();
            }
            if (result.outcome() == DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED) {
                model.addAttribute("conflictMessage", result.message());
                populatePanelModel(model, result.emergency(), result.refreshedCandidates());
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                return "dispatch/supervised";
            }
            model.addAttribute("waitingMessage", result.message());
            populatePanelModel(model, result.emergency(), List.of());
            return "dispatch/supervised";
        } catch (DispatchValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "dispatch/supervised";
        }
    }

    @PostMapping("/{id}/wait")
    public String moveToWaitingQueue(
            @PathVariable("id") String id,
            Model model,
            HttpServletResponse response
    ) {
        try {
            SupervisedDispatchResult result = dispatchApplicationService.moveToWaitingWhenNoEligible(id);
            if (result.outcome() == DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE) {
                model.addAttribute("waitingMessage", result.message());
            } else {
                model.addAttribute("conflictMessage", result.message());
            }
            populatePanelModel(model, result.emergency(), result.refreshedCandidates());
            return "dispatch/supervised";
        } catch (DispatchValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "dispatch/supervised";
        }
    }

    private static void populatePanelModel(
            Model model,
            Emergency emergency,
            List<EligibleAmbulanceCandidate> candidates
    ) {
        List<EligibleAmbulanceView> views = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            views.add(EligibleAmbulanceView.fromDomain(candidates.get(i), i == 0));
        }
        model.addAttribute("emergency", EmergencyView.fromDomain(emergency));
        model.addAttribute("candidates", views);
        model.addAttribute("hasSupplementalNotice",
                views.stream().anyMatch(EligibleAmbulanceView::supplementalSpecialized));
    }
}
