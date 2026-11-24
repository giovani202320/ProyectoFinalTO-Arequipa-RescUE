package com.arequiparescue.shared.presentation;

import com.arequiparescue.shared.application.HomeDashboardApplicationService;
import com.arequiparescue.shared.application.HomeDashboardSnapshot;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Objects;

/**
 * Controlador MVC de la página de inicio ({@code GET /}) que adapta el snapshot operativo de aplicación
 * al DTO de presentación {@link HomeStatusView} (P1-3).
 */
@Controller
public class HomeController {

    private static final String SYSTEM_TITLE = "ARE — Arequipa RescUE";
    private static final String ACADEMIC_DESCRIPTION =
            "Simulador académico de atención de emergencias médicas, despacho concurrente de ambulancias, "
                    + "cálculo de rutas mediante Dijkstra sobre una red vial simplificada de Arequipa y gestión "
                    + "simulada de capacidad hospitalaria.";
    private static final String ARCHITECTURE_SUMMARY =
            "Monolito modular con principios de arquitectura hexagonal y presentación Spring MVC + Thymeleaf";

    private final HomeDashboardApplicationService dashboardService;

    public HomeController(HomeDashboardApplicationService dashboardService) {
        this.dashboardService = Objects.requireNonNull(dashboardService);
    }

    @GetMapping("/")
    public String home(Model model) {
        HomeDashboardSnapshot snapshot = dashboardService.loadDashboardSnapshot();
        model.addAttribute("status", toView(snapshot));
        return "home";
    }

    static HomeStatusView toView(HomeDashboardSnapshot snapshot) {
        String profileTone = switch (snapshot.environmentProfile().kind()) {
            case DEMO -> "primary";
            case HISTORICAL -> "navy";
            case MYSQL_TEST -> "warning";
            case CUSTOM, UNIDENTIFIED -> "neutral";
        };

        List<HomeStatusView.StateDistributionRow> distributionRows = snapshot.stateDistribution().stream()
                .map(item -> new HomeStatusView.StateDistributionRow(
                        item.stateCode(),
                        item.count(),
                        item.sharePercent(),
                        resolveBarToneClass(item.stateCode())
                ))
                .toList();

        List<HomeStatusView.OperationalNoticeView> notices = snapshot.notices().stream()
                .map(n -> new HomeStatusView.OperationalNoticeView(
                        resolveAlertClass(n.severity()),
                        n.message(),
                        n.actionLabel(),
                        n.actionPath()
                ))
                .toList();

        HomeDashboardSnapshot.ActiveScenarioInfo active = snapshot.activeScenario();
        return new HomeStatusView(
                SYSTEM_TITLE,
                ACADEMIC_DESCRIPTION,
                ARCHITECTURE_SUMMARY,
                snapshot.environmentProfile().displayLabel(),
                profileTone,
                snapshot.environmentProfile().detailNote(),
                snapshot.globalLogicalTicks(),
                active != null,
                active != null ? active.scenarioId() : null,
                active != null ? active.name() : null,
                active != null ? active.scenarioLogicalTicks() : null,
                snapshot.totalEmergencies(),
                snapshot.activeEmergencies(),
                snapshot.completedEmergencies(),
                snapshot.waitingAmbulanceCount(),
                snapshot.waitingHospitalCount(),
                distributionRows,
                snapshot.stateDistributionConsistent(),
                snapshot.totalAmbulances(),
                snapshot.availableAmbulances(),
                snapshot.occupiedAmbulances(),
                snapshot.totalHospitals(),
                snapshot.enabledHospitals(),
                snapshot.disabledHospitals(),
                snapshot.enabledCapacityTotal(),
                snapshot.enabledReservedSlots(),
                snapshot.enabledOccupiedSlots(),
                snapshot.enabledAvailableCapacity(),
                snapshot.hospitalCapacityConsistent(),
                snapshot.inconsistentHospitalsCount(),
                snapshot.emptyDatabase(),
                notices
        );
    }

    private static String resolveBarToneClass(String stateCode) {
        return switch (stateCode) {
            case "COMPLETED" -> "bg-success";
            case "WAITING_AMBULANCE", "WAITING_HOSPITAL" -> "bg-warning";
            case "IN_TRANSPORT", "ASSIGNED", "AT_INCIDENT" -> "bg-primary";
            default -> "bg-secondary";
        };
    }

    private static String resolveAlertClass(String severity) {
        return switch (severity) {
            case "danger" -> "alert-danger";
            case "warning" -> "alert-warning";
            default -> "alert-info";
        };
    }
}
