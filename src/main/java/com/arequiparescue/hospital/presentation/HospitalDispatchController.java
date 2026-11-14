package com.arequiparescue.hospital.presentation;

import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.presentation.EmergencyView;
import com.arequiparescue.hospital.application.HospitalCatalogValidationException;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

/**
 * Controlador Spring MVC para la confirmación de llegada al incidente ({@code ASSIGNED -> AT_INCIDENT}),
 * la selección y reserva hospitalaria concurrente (HU-09) y la gestión FIFO de la cola
 * {@code WAITING_HOSPITAL} (HU-10) por el Despachador ({@code ACT-02}).
 */
@Controller
@RequestMapping("/dispatch")
public class HospitalDispatchController {

    private final HospitalDispatchApplicationService hospitalDispatchApplicationService;

    public HospitalDispatchController(HospitalDispatchApplicationService hospitalDispatchApplicationService) {
        this.hospitalDispatchApplicationService = Objects.requireNonNull(hospitalDispatchApplicationService);
    }

    @GetMapping("/hospital-queue")
    public String showHospitalQueue(Model model) {
        populateQueueModel(model);
        return "dispatch/hospital-queue";
    }

    @PostMapping("/emergencies/{id}/arrive")
    public String confirmArrivalAndReserveHospital(
            @PathVariable("id") String id,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        try {
            HospitalReservationResult result =
                    hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(id);
            String targetId = result.emergency().id().value();

            if (result.outcome() == HospitalDispatchOutcome.IN_TRANSPORT) {
                redirectAttributes.addFlashAttribute("hospitalDispatchMessage", result.message());
            } else if (result.outcome() == HospitalDispatchOutcome.HOSPITAL_RESERVATION_RETRY_EXHAUSTED) {
                redirectAttributes.addFlashAttribute("hospitalExhaustedMessage", result.message());
            } else {
                redirectAttributes.addFlashAttribute("hospitalWaitingMessage", result.message());
            }
            return "redirect:/emergencies/" + targetId;
        } catch (HospitalCatalogValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            populateQueueModel(model);
            return "dispatch/hospital-queue";
        }
    }

    @PostMapping("/hospital-queue/evaluate")
    public String evaluateWaitingHospitalQueue(Model model) {
        WaitingHospitalQueueEvaluationResult evaluation =
                hospitalDispatchApplicationService.evaluateWaitingHospitalQueue();
        model.addAttribute("evaluationResult", evaluation);
        populateQueueModel(model);
        return "dispatch/hospital-queue";
    }

    private void populateQueueModel(Model model) {
        List<HospitalView> hospitals = hospitalDispatchApplicationService.listHospitalsOrderById().stream()
                .map(HospitalView::fromDomain)
                .toList();
        List<EmergencyView> assignedEmergencies = hospitalDispatchApplicationService.listAssignedEmergencies().stream()
                .map(EmergencyView::fromDomain)
                .toList();
        List<EmergencyView> waitingHospitalQueue = hospitalDispatchApplicationService.listWaitingHospitalQueue().stream()
                .map(EmergencyView::fromDomain)
                .toList();
        List<DispatchTraceabilityRepository.ExhaustedDispatchNotice> exhaustedNotices =
                hospitalDispatchApplicationService.listRecentHospitalExhaustedNotices();

        model.addAttribute("hospitals", hospitals);
        model.addAttribute("assignedEmergencies", assignedEmergencies);
        model.addAttribute("waitingHospitalQueue", waitingHospitalQueue);
        model.addAttribute("exhaustedNotices", exhaustedNotices);
    }
}
