package com.arequiparescue.simulation.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.application.SimulationStepOutcome;
import com.arequiparescue.simulation.application.SimulationStepResult;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationPanelViewHelper — Proyección operativa y comparación real Antes/Después (P1-1)")
class SimulationPanelViewHelperTest {

    @Test
    @DisplayName("Detecta cambios reales de emergencia, hospital y reserva sin inventar deltas cuando no hay cambios")
    void computesRealBeforeAfterDeltasAndDetectsNoOpEvents() {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_A, SimulatedPriority.HIGH, EquipmentType.SPECIALIZED, TriageRuleVersionId.of("RUL-V01"));
        Emergency beforeEmg = Emergency.register(
                        EmergencyId.of("EMG-00001"), NodeId.of("N02"), "Crítico", triage, LogicalTime.ZERO)
                .assignAmbulance("AMB-01", LogicalTime.of(1L));
        Emergency afterEmg = beforeEmg
                .arriveAtIncident(LogicalTime.of(5L))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(5L));

        Ambulance amb = Ambulance.registerInCatalog(AmbulanceId.of("AMB-01"), EquipmentType.SPECIALIZED, NodeId.of("N05"));
        Hospital beforeHosp = Hospital.reconstruct(HospitalId.of("HOSP-01"), "Central", NodeId.of("N05"), true, 5, 0, 0);
        Hospital afterHosp = Hospital.reconstruct(HospitalId.of("HOSP-01"), "Central", NodeId.of("N05"), true, 5, 1, 0);
        HospitalReservation afterRes = HospitalReservation.reconstruct(
                "RES-EMG-00001", EmergencyId.of("EMG-00001"), HospitalId.of("HOSP-01"),
                HospitalReservationStatus.RESERVED, LogicalTime.of(5L), null, null);

        SimulationPanelViewHelper.RawOperationalState before = new SimulationPanelViewHelper.RawOperationalState(
                List.of(beforeEmg), List.of(amb), List.of(beforeHosp), List.of());
        SimulationPanelViewHelper.RawOperationalState after = new SimulationPanelViewHelper.RawOperationalState(
                List.of(afterEmg), List.of(amb), List.of(afterHosp), List.of(afterRes));

        ScheduledSimulationEvent ev = ScheduledSimulationEvent.pending(
                "SCN-01",
                new DiscreteEvent("EVT-01", LogicalTime.of(5L), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, 1L, "EMG-00001", "Llegada"));
        SimulationStepResult stepResult = new SimulationStepResult(
                "SCN-01", SimulationStepOutcome.EXECUTED, LogicalTime.of(1L), LogicalTime.of(5L), ev, null, "OK");

        SimulationPanelViewHelper.StepComparisonView withChanges =
                SimulationPanelViewHelper.buildStepComparison(before, after, stepResult);
        assertThat(withChanges.hasOperationalChanges()).isTrue();
        assertThat(withChanges.deltas()).hasSize(3);
        assertThat(withChanges.deltas()).extracting(SimulationPanelViewHelper.ResourceDelta::resourceId)
                .containsExactly("EMG-00001", "HOSP-01", "RES-EMG-00001");

        SimulationPanelViewHelper.StepComparisonView noChanges =
                SimulationPanelViewHelper.buildStepComparison(before, before, stepResult);
        assertThat(noChanges.hasOperationalChanges()).isFalse();
        assertThat(noChanges.deltas()).isEmpty();
    }
}
