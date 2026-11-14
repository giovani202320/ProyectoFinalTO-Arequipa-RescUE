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
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Ejecutor de intentos atómicos para llegada al incidente (HU-09) y reserva hospitalaria
 * bajo bloqueo pesimista {@code SELECT ... FOR UPDATE} en orden canónico
 * {@code Emergency -> Ambulance -> Hospital} (ADR-0002 Decisiones 2, 3 y 7, INV-SM-08).
 */
@Component
public class HospitalReservationAttemptExecutor {

    public record ArrivalOutcome(
            boolean arrived,
            boolean duplicateOrInvalid,
            Emergency emergency,
            Ambulance ambulance,
            String message
    ) {
    }

    public record ReservationAttemptOutcome(
            boolean succeeded,
            boolean conflict,
            Emergency emergency,
            Ambulance ambulance,
            Hospital hospital,
            String reason
    ) {
    }

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalReservationRepository hospitalReservationRepository;

    public HospitalReservationAttemptExecutor(
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
     * Ejecuta atómicamente T-EME-04 (ASSIGNED -> AT_INCIDENT) y T-AMB-02 (ASSIGNED -> AT_INCIDENT).
     * Rechaza de forma idempotente invocaciones duplicadas si ya no está en ASSIGNED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public ArrivalOutcome confirmArrivalAtIncident(EmergencyId emergencyId, LogicalTime at) {
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));

        if (emergency.state() != EmergencyState.ASSIGNED || emergency.assignedAmbulanceId() == null) {
            return new ArrivalOutcome(
                    false,
                    true,
                    emergency,
                    null,
                    "Evento de llegada duplicado o estado no válido: la emergencia " + emergencyId.value()
                            + " se encuentra en estado " + emergency.state() + "."
            );
        }

        AmbulanceId ambulanceId = AmbulanceId.of(emergency.assignedAmbulanceId());
        Ambulance ambulance = ambulanceRepository.findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe la ambulancia asignada " + ambulanceId.value() + "."));

        if (ambulance.state() != AmbulanceState.ASSIGNED
                || !emergencyId.equals(ambulance.assignedEmergencyId())) {
            return new ArrivalOutcome(
                    false,
                    true,
                    emergency,
                    ambulance,
                    "Estado inconsistente o evento duplicado sobre la ambulancia " + ambulanceId.value()
                            + " (estado actual: " + ambulance.state() + ")."
            );
        }

        Emergency arrivedEmergency = emergencyRepository.save(emergency.arriveAtIncident(at));
        Ambulance arrivedAmbulance = ambulanceRepository.save(ambulance.arriveAtIncident(emergency.locationNode()));

        return new ArrivalOutcome(
                true,
                false,
                arrivedEmergency,
                arrivedAmbulance,
                "Llegada al incidente confirmada: " + arrivedEmergency.id().value()
                        + " y " + arrivedAmbulance.id().value() + " en AT_INCIDENT."
        );
    }

    /**
     * Ejecuta un intento atómico de reserva hospitalaria bajo orden canónico
     * {@code Emergency -> Ambulance -> Hospital} ({@code SELECT ... FOR UPDATE}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public ReservationAttemptOutcome executeHospitalReservationAttempt(
            EmergencyId emergencyId,
            HospitalId hospitalId,
            LogicalTime at
    ) {
        // 1.° Emergency
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));

        if ((emergency.state() != EmergencyState.AT_INCIDENT && emergency.state() != EmergencyState.WAITING_HOSPITAL)
                || emergency.assignedAmbulanceId() == null) {
            return new ReservationAttemptOutcome(
                    false,
                    false,
                    emergency,
                    null,
                    null,
                    "La emergencia " + emergencyId.value() + " no admite reserva hospitalaria en estado " + emergency.state() + "."
            );
        }

        // 2.° Ambulance
        AmbulanceId ambulanceId = AmbulanceId.of(emergency.assignedAmbulanceId());
        Ambulance ambulance = ambulanceRepository.findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe la ambulancia retenida " + ambulanceId.value() + "."));

        if (ambulance.state() != AmbulanceState.AT_INCIDENT
                || !emergencyId.equals(ambulance.assignedEmergencyId())) {
            return new ReservationAttemptOutcome(
                    false,
                    false,
                    emergency,
                    ambulance,
                    null,
                    "La ambulancia " + ambulanceId.value() + " no está retenida en AT_INCIDENT para " + emergencyId.value() + "."
            );
        }

        if (hospitalReservationRepository.existsByEmergencyId(emergencyId)) {
            return new ReservationAttemptOutcome(
                    false,
                    false,
                    emergency,
                    ambulance,
                    null,
                    "La emergencia " + emergencyId.value() + " ya posee una reserva hospitalaria registrada (CA-HOS-04 / unicidad)."
            );
        }

        // 3.° Hospital
        Hospital hospital = hospitalRepository.findByIdForUpdate(hospitalId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe el hospital " + hospitalId.value() + "."));

        if (!hospital.enabled() || hospital.availableSlots() < 1) {
            return new ReservationAttemptOutcome(
                    false,
                    true,
                    emergency,
                    ambulance,
                    hospital,
                    "HOSPITAL_CAPACITY_CONFLICT: el hospital " + hospitalId.value()
                            + " ya no tiene plazas disponibles (enabled=" + hospital.enabled()
                            + ", D=" + hospital.availableSlots() + ")."
            );
        }

        Hospital updatedHospital = hospitalRepository.save(hospital.reserveSlot());
        hospitalReservationRepository.createReserved(
                "RES-" + emergencyId.value(),
                emergencyId,
                hospitalId,
                at.ticks()
        );
        Emergency inTransportEmergency = emergencyRepository.save(
                emergency.reserveHospitalAndStartTransport(hospitalId.value(), at)
        );
        Ambulance transportingAmbulance = ambulanceRepository.save(ambulance.startTransportToHospital());

        return new ReservationAttemptOutcome(
                true,
                false,
                inTransportEmergency,
                transportingAmbulance,
                updatedHospital,
                "Reserva hospitalaria confirmada en " + updatedHospital.id().value()
                        + " (R=" + updatedHospital.reservedSlots()
                        + ", D=" + updatedHospital.availableSlots()
                        + "); traslado iniciado (IN_TRANSPORT / TRANSPORTING)."
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)
    public Emergency moveEmergencyToWaitingHospital(
            EmergencyId emergencyId,
            LogicalTime at,
            String reason
    ) {
        Emergency emergency = emergencyRepository.findByIdForUpdate(emergencyId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));
        if (emergency.state() != EmergencyState.AT_INCIDENT
                && emergency.state() != EmergencyState.WAITING_HOSPITAL) {
            return emergency;
        }
        return emergencyRepository.save(emergency.enterOrRemainInWaitingHospital(at, reason));
    }
}
