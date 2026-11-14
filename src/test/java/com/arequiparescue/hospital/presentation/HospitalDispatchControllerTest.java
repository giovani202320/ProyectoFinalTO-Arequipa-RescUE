package com.arequiparescue.hospital.presentation;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.WaitingHospitalQueueEvaluationResult;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HospitalDispatchController.class)
@DisplayName("HospitalDispatchController — Vistas MVC de llegada al incidente y cola WAITING_HOSPITAL (HU-09, HU-10)")
class HospitalDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospitalDispatchApplicationService hospitalDispatchApplicationService;

    private static Emergency sampleWaitingEmergency() {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01"));
        return Emergency.register(EmergencyId.of("EMG-00001"), NodeId.of("N01"), "Dolor torácico", triage, LogicalTime.of(1L))
                .assignAmbulance("AMB-01", LogicalTime.of(2L))
                .arriveAtIncident(LogicalTime.of(3L))
                .enterOrRemainInWaitingHospital(LogicalTime.of(4L), "NO_ELIGIBLE_HOSPITAL");
    }

    @Test
    @DisplayName("GET /dispatch/hospital-queue muestra disponibilidad C/R/O/D, emergencias ASSIGNED y cola WAITING_HOSPITAL")
    void shouldRenderHospitalQueueView() throws Exception {
        Hospital h1 = Hospital.registerInCatalog(HospitalId.of("HOSP-01"), "Hospital Goyeneche", NodeId.of("N04"), true, 2);
        when(hospitalDispatchApplicationService.listHospitalsOrderById()).thenReturn(List.of(h1));
        when(hospitalDispatchApplicationService.listAssignedEmergencies()).thenReturn(List.of());
        when(hospitalDispatchApplicationService.listWaitingHospitalQueue()).thenReturn(List.of(sampleWaitingEmergency()));
        when(hospitalDispatchApplicationService.listRecentHospitalExhaustedNotices()).thenReturn(List.of());

        mockMvc.perform(get("/dispatch/hospital-queue"))
                .andExpect(status().isOk())
                .andExpect(view().name("dispatch/hospital-queue"))
                .andExpect(model().attributeExists("hospitals", "assignedEmergencies", "waitingHospitalQueue", "exhaustedNotices"));
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
}
