package com.arequiparescue.dashboard.presentation;

import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.dashboard.application.HomeDashboardApplicationService;
import com.arequiparescue.dashboard.application.HomeDashboardSnapshot;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(AreUiFormatter.class)
@DisplayName("HomeController — Renderizado de GET / con dashboard operativo (P1-3)")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeDashboardApplicationService dashboardService;

    @Test
    @DisplayName("GET / renderiza indicadores operativos reales, perfil, relojes diferenciados, accesos rápidos y sin Incremento 01")
    void shouldRenderPopulatedOperationalHomeDashboard() throws Exception {
        HomeDashboardSnapshot snapshot = HomeDashboardApplicationService.buildSnapshot(
                List.of(EmergencyState.WAITING_AMBULANCE, EmergencyState.IN_TRANSPORT, EmergencyState.COMPLETED),
                List.of(AmbulanceState.AVAILABLE, AmbulanceState.TRANSPORTING),
                List.of(
                        new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-01", true, 5, 1, 2),
                        new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-02", false, 3, 0, 0)
                ),
                new HomeDashboardSnapshot.ActiveScenarioInfo("SCN-01", "Demostración académica", 9L),
                15L,
                new String[]{"demo"}
        );
        when(dashboardService.loadDashboardSnapshot()).thenReturn(snapshot);

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("ARE — Arequipa RescUE")))
                .andExpect(content().string(containsString("Simulador académico de atención de emergencias médicas")))
                .andExpect(content().string(containsString("Centro de operaciones")))
                .andExpect(content().string(containsString("Estado operativo actual")))
                .andExpect(content().string(containsString("Perfil activo: demo")))
                .andExpect(content().string(containsString("Reloj global: t=15")))
                .andExpect(content().string(containsString("Escenario activo: SCN-01 (t=9)")))
                .andExpect(content().string(containsString("1 / 2")))
                .andExpect(content().string(containsString("D=2 / C=5")))
                .andExpect(content().string(containsString("Distribución de emergencias por estado")))
                .andExpect(content().string(containsString("Esperando ambulancia")))
                .andExpect(content().string(containsString("Topología / Dijkstra")))
                .andExpect(content().string(containsString("Módulos operativos")))
                .andExpect(content().string(containsString("/emergencies/new")))
                .andExpect(content().string(containsString("/admin/ambulances")))
                .andExpect(content().string(containsString("/dispatch/waiting-queue")))
                .andExpect(content().string(containsString("/admin/hospitals")))
                .andExpect(content().string(containsString("/simulation")))
                .andExpect(content().string(containsString("/admin/topology")))
                .andExpect(content().string(containsString("/reporting")))
                .andExpect(content().string(not(containsString("Incremento 01"))))
                .andExpect(content().string(containsString("/vendor/bootstrap/5.3.3/css/bootstrap.min.css")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("/vendor/lucide/0.469.0/lucide.min.js")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        HtmlStructureAssertions.assertSingleAppShellStructure(html, "are-hero-title");
    }

    @Test
    @DisplayName("GET / renderiza estado vacío comprensible cuando no hay registros en la base")
    void shouldRenderEmptyStateCleanly() throws Exception {
        HomeDashboardSnapshot empty = HomeDashboardApplicationService.buildSnapshot(
                List.of(),
                List.of(),
                List.of(),
                null,
                0L,
                new String[0]
        );
        when(dashboardService.loadDashboardSnapshot()).thenReturn(empty);

        String html = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Base de datos sin registros operativos iniciales")))
                .andExpect(content().string(containsString("Perfil activo: no identificado (default)")))
                .andExpect(content().string(containsString("Sin escenario activo")))
                .andExpect(content().string(not(containsString("Incremento 01"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        HtmlStructureAssertions.assertSingleAppShellStructure(html, "are-hero-title");
    }
}
