package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.EmergencyNotFoundException;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador Spring MVC para el registro y clasificación determinista de emergencias ({@code HU-01})
 * y la consulta/seguimiento de sus estados aprobados ({@code HU-02}).
 */
@Controller
@RequestMapping("/emergencies")
public class EmergencyController {

    private final EmergencyApplicationService emergencyApplicationService;

    public EmergencyController(EmergencyApplicationService emergencyApplicationService) {
        this.emergencyApplicationService = emergencyApplicationService;
    }

    @GetMapping
    public String listEmergencies(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "state", required = false) String stateFilter,
            Model model
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        String normalizedState = stateFilter == null ? "" : stateFilter.trim().toUpperCase();

        List<EmergencyView> allEmergencies = emergencyApplicationService.listEmergencies()
                .stream()
                .map(EmergencyView::fromDomain)
                .toList();

        List<EmergencyView> filtered = allEmergencies.stream()
                .filter(item -> normalizedQuery.isEmpty()
                        || item.id().toUpperCase().contains(normalizedQuery.toUpperCase()))
                .filter(item -> normalizedState.isEmpty()
                        || item.stateCode().equalsIgnoreCase(normalizedState))
                .toList();

        model.addAttribute("emergencies", filtered);
        model.addAttribute("totalEmergenciesCount", allEmergencies.size());
        model.addAttribute("searchQuery", normalizedQuery);
        model.addAttribute("selectedState", normalizedState);
        model.addAttribute("states", EmergencyState.values());
        model.addAttribute("currentLogicalTicks", emergencyApplicationService.currentLogicalTime().ticks());
        model.addAttribute("activeRuleVersionId", resolveActiveRuleVersionId());
        return "emergencies/list";
    }

    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        model.addAttribute("form", new RegisterEmergencyForm());
        populateFormReferenceData(model);
        return "emergencies/form";
    }

    @PostMapping
    public String registerEmergency(
            @Valid @ModelAttribute("form") RegisterEmergencyForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormReferenceData(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "emergencies/form";
        }

        try {
            Emergency saved = emergencyApplicationService.registerEmergency(new RegisterEmergencyCommand(
                    form.getIncidentCategory(),
                    form.getNodeId(),
                    form.getDescription()
            ));
            redirectAttributes.addFlashAttribute("justRegistered", true);
            return "redirect:/emergencies/" + saved.id().value();
        } catch (EmergencyValidationException ex) {
            if ("incidentCategory".equals(ex.field())
                    || "nodeId".equals(ex.field())
                    || "description".equals(ex.field())) {
                bindingResult.rejectValue(ex.field(), "emergency.validation", ex.getMessage());
            } else {
                bindingResult.reject("emergency.validation", ex.getMessage());
            }
            populateFormReferenceData(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "emergencies/form";
        }
    }

    @GetMapping("/{id}")
    public String showEmergencyDetail(@PathVariable("id") String id, Model model) {
        Emergency emergency = emergencyApplicationService.getEmergencyById(id);
        model.addAttribute("emergency", EmergencyView.fromDomain(emergency));
        if (!model.containsAttribute("justRegistered")) {
            model.addAttribute("justRegistered", false);
        }
        return "emergencies/detail";
    }

    @ExceptionHandler(EmergencyNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEmergencyNotFound(EmergencyNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("emergencies", emergencyApplicationService.listEmergencies()
                .stream()
                .map(EmergencyView::fromDomain)
                .toList());
        model.addAttribute("currentLogicalTicks", emergencyApplicationService.currentLogicalTime().ticks());
        model.addAttribute("activeRuleVersionId", resolveActiveRuleVersionId());
        return "emergencies/list";
    }

    private void populateFormReferenceData(Model model) {
        List<CategoryOptionView> categories = Arrays.stream(IncidentCategory.values())
                .map(category -> new CategoryOptionView(category.code(), category.displayLabel()))
                .toList();
        List<NodeOptionView> nodes = emergencyApplicationService.listConfiguredGraphNodes()
                .stream()
                .map(node -> new NodeOptionView(node.id().value(), node.displayName(), node.isolatedInBaseTopology()))
                .toList();

        model.addAttribute("categories", categories);
        model.addAttribute("nodes", nodes);
        model.addAttribute("activeRuleVersionId", resolveActiveRuleVersionId());
        model.addAttribute("currentLogicalTicks", emergencyApplicationService.currentLogicalTime().ticks());
    }

    private String resolveActiveRuleVersionId() {
        return emergencyApplicationService.findActiveRuleVersion()
                .map(TriageRuleVersion::id)
                .map(Object::toString)
                .orElse("SIN_VERSION_ACTIVA");
    }
}
