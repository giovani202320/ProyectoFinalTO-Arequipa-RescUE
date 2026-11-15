package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Ejecutor transaccional corto e independiente ({@code REQUIRES_NEW}, {@code READ_COMMITTED})
 * para la entrega hospitalaria atómica (HU-11: {@code T-EME-08} + {@code T-AMB-04} + {@code R -> O})
 * y el alta simulada idempotente (HU-12: {@code OCCUPIED -> DISCHARGED} + {@code O = O - 1}),
 * aplicando bloqueos pesimistas {@code SELECT ... FOR UPDATE} conforme a ADR-0002.
 */
@Component
public class HospitalDeliveryAndDischargeAttemptExecutor {

    public record DeliveryExecutionOutcome(
            boolean completed,
            Emergency emergency,
            Ambulance releasedAmbulance,
            Hospital hospital,
            HospitalReservation reservation,
            String message
    ) {
    }

    public record DischargeExecutionOutcome(
            boolean discharged,
            Hospital hospital,
            HospitalReservation reservation,
            String message
    ) {
    }

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalReservationRepository hospitalReservationRepository;

    public HospitalDeliveryAndDischargeAttemptExecutor(
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            HospitalRepository hospitalRepository,
            HospitalReservationRepository hospitalReservationRepository
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.hospitalRepository = Objects.requireNonNull(hospitalRepository);
        this.hospitalReservationRepository = Objects.requireNonNull(hospitalReservationRepository);
    }

    /**
     * HU-11: Ejecuta atómicamente IN_TRANSPORT -> COMPLETED, TRANSPORTING -> AVAILABLE en el nodo
     * del hospital, RESERVED -> OCCUPIED y R = R - 1, O = O + 1 bajo orden canónico
     * Emergency -> Ambulance -> Hospital -> HospitalReservation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public DeliveryExecutionOutcome executeHospitalDelivery(EmergencyId emergencyId, LogicalTime at) {
        // 1.° Emergency
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));

        if (emergency.state() != EmergencyState.IN_TRANSPORT
                || emergency.assignedAmbulanceId() == null
                || emergency.reservedOrOccupiedHospitalId() == null) {
            return new DeliveryExecutionOutcome(
                    false,
                    emergency,
                    null,
                    null,
                    null,
                    "Entrega hospitalaria duplicada o estado no válido: la emergencia " + emergencyId.value()
                            + " se encuentra en estado " + emergency.state() + "."
            );
        }

        // 2.° Ambulance
        AmbulanceId ambulanceId = AmbulanceId.of(emergency.assignedAmbulanceId());
        Ambulance ambulance = ambulanceRepository.findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ambulancia vinculada " + ambulanceId.value() + " para entrega hospitalaria."));
        if (ambulance.state() != AmbulanceState.TRANSPORTING
                || !emergency.id().equals(ambulance.assignedEmergencyId())) {
            throw new IllegalStateException(
                    "Inconsistencia operativa: la ambulancia " + ambulanceId.value()
                            + " no se encuentra TRANSPORTING para " + emergencyId.value() + ".");
        }

        // 3.° Hospital
        HospitalId hospitalId = HospitalId.of(emergency.reservedOrOccupiedHospitalId());
        Hospital hospital = hospitalRepository.findByIdForUpdate(hospitalId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el hospital reservado " + hospitalId.value() + "."));

        // 4.° HospitalReservation (debe estar RESERVED)
        HospitalReservation reservation = hospitalReservationRepository.findByEmergencyIdForUpdate(emergencyId)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe reserva hospitalaria activa para la emergencia " + emergencyId.value() + "."));
        if (reservation.status() != HospitalReservationStatus.RESERVED
                || !reservation.hospitalId().equals(hospitalId)) {
            return new DeliveryExecutionOutcome(
                    false,
                    emergency,
                    ambulance,
                    hospital,
                    reservation,
                    "Entrega hospitalaria duplicada o reserva no válida: la reserva " + reservation.id()
                            + " se encuentra en estado " + reservation.status() + "."
            );
        }

        Emergency completedEmergency = emergency.completeHospitalDelivery(at);
        Ambulance releasedAmbulance = ambulance.completeDeliveryAtHospital(hospital.locationNode());
        Hospital updatedHospital = hospital.convertReservationToOccupation();
        HospitalReservation occupiedReservation = reservation.occupy(at);

        Emergency savedEmergency = emergencyRepository.save(completedEmergency);
        Ambulance savedAmbulance = ambulanceRepository.save(releasedAmbulance);
        Hospital savedHospital = hospitalRepository.save(updatedHospital);
        HospitalReservation savedReservation = hospitalReservationRepository.save(occupiedReservation);

        return new DeliveryExecutionOutcome(
                true,
                savedEmergency,
                savedAmbulance,
                savedHospital,
                savedReservation,
                "Entrega hospitalaria confirmada para " + savedEmergency.id().value()
                        + " en " + savedHospital.id().value()
                        + " (R=" + savedHospital.reservedSlots() + ", O=" + savedHospital.occupiedSlots()
                        + ", D=" + savedHospital.availableSlots() + "). Ambulancia "
                        + savedAmbulance.id().value() + " liberada (AVAILABLE) en nodo "
                        + savedAmbulance.currentNode().value() + "."
        );
    }

    /**
     * HU-12: Ejecuta idempotentemente el alta simulada sobre una ocupación OCCUPIED -> DISCHARGED
     * y decrementa O = O - 1 (incrementando D = D + 1), sin modificar Emergency ni Ambulance.
     * Respeta el orden global de bloqueo de ADR-0002 (Hospital -> HospitalReservation) con contexto
     * de persistencia limpio para leer el estado fresco tras adquirir cada bloqueo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public DischargeExecutionOutcome executeSimulatedDischarge(
            String reservationId,
            HospitalId hospitalId,
            LogicalTime at
    ) {
        // 1.° Hospital (orden global canónico: Hospital -> HospitalReservation)
        Hospital hospital = hospitalRepository.findByIdForUpdate(hospitalId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el hospital " + hospitalId.value() + " para alta simulada."));

        // 2.° HospitalReservation (lectura fresca con PESSIMISTIC_WRITE en EntityManager limpio)
        HospitalReservation reservation = hospitalReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna reserva u ocupación hospitalaria con identificador " + reservationId + "."));

        if (reservation.status() != HospitalReservationStatus.OCCUPIED
                || !reservation.hospitalId().equals(hospital.id())) {
            return new DischargeExecutionOutcome(
                    false,
                    hospital,
                    reservation,
                    "Alta simulada rechazada o duplicada: la reserva " + reservation.id()
                            + " se encuentra en estado " + reservation.status()
                            + " (solo se permite sobre estado OCCUPIED)."
            );
        }

        Hospital updatedHospital = hospital.dischargeOccupiedSlot();
        HospitalReservation dischargedReservation = reservation.discharge(at);

        Hospital savedHospital = hospitalRepository.save(updatedHospital);
        HospitalReservation savedReservation = hospitalReservationRepository.save(dischargedReservation);

        return new DischargeExecutionOutcome(
                true,
                savedHospital,
                savedReservation,
                "Alta simulada ejecutada sobre ocupación " + savedReservation.id()
                        + " (" + savedHospital.id().value() + ": O=" + savedHospital.occupiedSlots()
                        + ", D=" + savedHospital.availableSlots() + ")."
        );
    }
}
