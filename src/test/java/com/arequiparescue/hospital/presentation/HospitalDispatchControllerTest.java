package com.arequiparescue.hospital.presentation;

import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryResult;
import com.arequiparescue.hospital.application.HospitalDischargeResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HospitalDispatchController.class)
@Import(AreUiFormatter.class)
@DisplayName("HospitalDispatchController — Vistas MVC de llegada, cola, entrega y alta hospitalaria (HU-09 a HU-12)")
class HospitalDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospitalDispatchApplicationService hospitalDispatchApplicationService;

    @MockitoBean
    private HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeApplicationService;

    private static Emergency sampleWaitingEmergency() {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01"));
        return Emergency.register(EmergencyId.of("EMG-00001"), NodeId.of("N01"), "Dolor torácico", triage, LogicalTime.of(1L))
                .assignAmbulance("AMB-01", LogicalTime.of(2L))
                .arriveAtIncident(LogicalTime.of(3L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(4L), "NO_ELIGIBLE_HOSPITAL");
    }

    @Test
    @DisplayName("GET /dispatch/hospital-queue muestra disponibilidad C/R/O/D, colas, traslados y reservas")
    void shouldRenderHospitalQueueView() throws Exception {
        Hospital h1 = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "Hospital Goyeneche", NodeId.of("N04"), true, 2);
        when(hospitalDispatchApplicationService.listHospitalsOrderById()).thenReturn(List.of(h1));
        when(hospitalDispatchApplicationService.listAssignedEmergencies()).thenReturn(List.of());
        when(hospitalDispatchApplicationService.listWaitingHospitalQueue()).thenReturn(List.of(sampleWaitingEmergency()));
        when(hospitalDispatchApplicationService.listRecentHospitalExhaustedNotices()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listInTransportEmergencies()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listCompletedEmergencies()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listHospitalReservations()).thenReturn(List.of());

        String html = mockMvc.perform(get("/dispatch/hospital-queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("dispatch/hospital-queue"))
                .andExpect(model().attributeExists(
                        "hospitals", "assignedEmergencies", "waitingHospitalQueue",
                        "inTransportEmergencies", "completedEmergencies", "hospitalReservations", "exhaustedNotices"))
                .andExpect(content().string(containsString("HOSP-01")))
                .andExpect(content().string(containsString("Habilitado")))
                .andExpect(content().string(containsString("href=\"/admin/hospitals\"")))
                .andExpect(content().string(not(containsString("href=\"/hospitals\""))))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(html, "hospital-queue-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form")).isEqualTo(1);

        String designSystemCss = new String(
                new ClassPathResource("static/css/are-design-system.css").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(designSystemCss).contains(".are-alert-info", ".are-badge-info", ".are-badge-icon");
    }

    @Test
    @DisplayName("POST /dispatch/hospital-queue/evaluate reevalúa cola FIFO y renderiza resultado")
    void shouldEvaluateWaitingHospitalQueue() throws Exception {
        WaitingHospitalQueueEvaluationResult eval =
                new WaitingHospitalQueueEvaluationResult(1, 1, 0, List.of(), "1 reserva(s) confirmada(s)");
        when(hospitalDispatchApplicationService.evaluateWaitingHospitalQueue()).thenReturn(eval);
        when(hospitalDispatchApplicationService.listHospitalsOrderById()).thenReturn(List.of());
        when(hospitalDispatchApplicationService.listAssignedEmergencies()).thenReturn(List.of());
        when(hospitalDispatchApplicationService.listWaitingHospitalQueue()).thenReturn(List.of());
        when(hospitalDispatchApplicationService.listRecentHospitalExhaustedNotices()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listInTransportEmergencies()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listCompletedEmergencies()).thenReturn(List.of());
        when(deliveryAndDischargeApplicationService.listHospitalReservations()).thenReturn(List.of());

        mockMvc.perform(post("/dispatch/hospital-queue/evaluate"))
                .andExpect(status().isOk())
                .andExpect(view().name("dispatch/hospital-queue"))
                .andExpect(model().attribute("evaluationResult", eval));
    }

    @Test
    @DisplayName("POST /dispatch/emergencies/{id}/arrive redirige al detalle con flash de éxito al reservar hospital")
    void shouldConfirmArrivalAndRedirectOnSuccess() throws Exception {
        Emergency inTransport = sampleWaitingEmergency().reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(5L));
        HospitalReservationResult result = new HospitalReservationResult(
                "HOSP-ARR-1", HospitalDispatchOutcome.IN_TRANSPORT, inTransport, null, null, 1, 0, "IN_TRANSPORT", "Reservado");
        when(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital("EMG-00001"))
                .thenReturn(result);

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/arrive"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/emergencies/EMG-00001"))
                .andExpect(flash().attributeExists("hospitalDispatchMessage"));
    }

    @Test
    @DisplayName("POST /dispatch/emergencies/{id}/deliver confirma entrega HU-11 y redirige con flash")
    void shouldConfirmHospitalDeliveryAndRedirectOnSuccess() throws Exception {
        Emergency completed = sampleWaitingEmergency()
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(5L))
                .completeHospitalDelivery(LogicalTime.of(6L));
        WaitingQueueEvaluationResult queueEval = new WaitingQueueEvaluationResult(0, 0, 0, List.of(), "Sin cola");
        HospitalDeliveryResult result = new HospitalDeliveryResult(
                "HOSP-DEL-1", HospitalDeliveryOutcome.COMPLETED, completed, null, null, null, queueEval, "Entregado");
        when(deliveryAndDischargeApplicationService.confirmHospitalDelivery("EMG-00001")).thenReturn(result);

        mockMvc.perform(post("/dispatch/emergencies/EMG-00001/deliver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/emergencies/EMG-00001"))
                .andExpect(flash().attributeExists("deliverySuccessMessage"));
    }

    @Test
    @DisplayName("POST /dispatch/hospital-reservations/{reservationId}/discharge ejecuta alta simulada HU-12 y redirige")
    void shouldExecuteSimulatedDischargeAndRedirect() throws Exception {
        WaitingHospitalQueueEvaluationResult hospEval =
                new WaitingHospitalQueueEvaluationResult(0, 0, 0, List.of(), "Sin espera");
        HospitalDischargeResult result = new HospitalDischargeResult(
                "HOSP-DIS-1", HospitalDischargeOutcome.DISCHARGED, null, null, hospEval, "Alta ejecutada");
        when(deliveryAndDischargeApplicationService.executeSimulatedDischarge("RES-1")).thenReturn(result);

        mockMvc.perform(post("/dispatch/hospital-reservations/RES-1/discharge"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/hospital-queue"))
                .andExpect(flash().attributeExists("dischargeSuccessMessage"));
    }
}
