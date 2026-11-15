package com.arequiparescue.hospital.domain;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HospitalDeliveryAndDischargeDomainTest — Transiciones de entrega HU-11 y alta simulada HU-12")
class HospitalDeliveryAndDischargeDomainTest {

    @Test
    @DisplayName("HU-11: convertReservationToOccupation ajusta R-1 y O+1 manteniendo D igual, incluso con D=0 o deshabilitado")
    void shouldConvertReservedSlotToOccupiedEvenWhenFullOrDisabled() {
        Hospital fullDisabled = Hospital.reconstruct(
                HospitalId.of("HOSP-01"), "Hospital Lleno", NodeId.of("N04"), false, 2, 1, 1);
        assertThat(fullDisabled.availableSlots()).isZero();

        Hospital occupied = fullDisabled.convertReservationToOccupation();
        assertThat(occupied.reservedSlots()).isZero();
        assertThat(occupied.occupiedSlots()).isEqualTo(2);
        assertThat(occupied.availableSlots()).isZero();
        assertThatThrownBy(occupied::convertReservationToOccupation).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("HU-12: dischargeOccupiedSlot ajusta O-1 y aumenta D+1, rechazando O=0 (sin valores negativos)")
    void shouldDischargeOccupiedSlotAndPreventNegativeOccupation() {
        Hospital hosp = Hospital.reconstruct(
                HospitalId.of("HOSP-01"), "Hospital Central", NodeId.of("N04"), true, 2, 0, 1);

        Hospital afterDischarge = hosp.dischargeOccupiedSlot();
        assertThat(afterDischarge.occupiedSlots()).isZero();
        assertThat(afterDischarge.availableSlots()).isEqualTo(2);
        assertThatThrownBy(afterDischarge::dischargeOccupiedSlot).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("HU-11 y HU-12: Emergency IN_TRANSPORT->COMPLETED, Ambulance TRANSPORTING->AVAILABLE en nodo hospital y reserva RESERVED->OCCUPIED->DISCHARGED")
    void shouldCompleteDeliveryAndDischargeReservationAcrossDomainEntities() {
        TriageClassification triage = new TriageClassification(
                IncidentCategory.INC_B, SimulatedPriority.MEDIUM, EquipmentType.BASIC, TriageRuleVersionId.of("RUL-V01"));
        Emergency inTransport = Emergency.register(
                        EmergencyId.of("EMG-00001"), NodeId.of("N01"), "Paciente", triage, LogicalTime.of(1L))
                .assignAmbulance("AMB-01", LogicalTime.of(2L))
                .arriveAtIncident(LogicalTime.of(3L))
                .reserveHospitalAndStartTransport("HOSP-01", LogicalTime.of(4L));
        Ambulance transporting = Ambulance.registerInCatalog(
                        AmbulanceId.of("AMB-01"), EquipmentType.BASIC, NodeId.of("N01"))
                .assignToEmergency(EmergencyId.of("EMG-00001"))
                .arriveAtIncident(NodeId.of("N01"))
                .startTransportToHospital();

        Emergency completed = inTransport.completeHospitalDelivery(LogicalTime.of(5L));
        Ambulance available = transporting.completeDeliveryAtHospital(NodeId.of("N04"));

        assertThat(completed.state()).isEqualTo(EmergencyState.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(LogicalTime.of(5L));
        assertThat(completed.assignedAmbulanceId()).isEqualTo("AMB-01");
        assertThat(completed.reservedOrOccupiedHospitalId()).isEqualTo("HOSP-01");
        assertThat(available.state()).isEqualTo(AmbulanceState.AVAILABLE);
        assertThat(available.currentNode()).isEqualTo(NodeId.of("N04"));
        assertThat(available.assignedEmergencyId()).isNull();

        assertThatThrownBy(() -> completed.completeHospitalDelivery(LogicalTime.of(6L)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> available.completeDeliveryAtHospital(NodeId.of("N04")))
                .isInstanceOf(IllegalStateException.class);

        HospitalReservation reserved = HospitalReservation.reconstruct(
                "RES-1", EmergencyId.of("EMG-00001"), HospitalId.of("HOSP-01"),
                HospitalReservationStatus.RESERVED, LogicalTime.of(4L), null, null);
        assertThatThrownBy(() -> reserved.discharge(LogicalTime.of(6L))).isInstanceOf(IllegalStateException.class);

        HospitalReservation occupied = reserved.occupy(LogicalTime.of(5L));
        assertThat(occupied.status()).isEqualTo(HospitalReservationStatus.OCCUPIED);
        assertThat(occupied.occupiedAt()).isEqualTo(LogicalTime.of(5L));

        HospitalReservation discharged = occupied.discharge(LogicalTime.of(7L));
        assertThat(discharged.status()).isEqualTo(HospitalReservationStatus.DISCHARGED);
        assertThat(discharged.dischargedAt()).isEqualTo(LogicalTime.of(7L));
        assertThatThrownBy(() -> discharged.discharge(LogicalTime.of(8L))).isInstanceOf(IllegalStateException.class);
    }
}
