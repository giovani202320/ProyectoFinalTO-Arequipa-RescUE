package com.arequiparescue.hospital.infrastructure.persistence;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Adaptador JPA para {@link HospitalReservationRepository}.
 */
@Repository
public class HospitalReservationJpaAdapter implements HospitalReservationRepository {

    private final SpringDataHospitalReservationRepository springDataRepository;

    public HospitalReservationJpaAdapter(SpringDataHospitalReservationRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmergencyId(EmergencyId emergencyId) {
        Objects.requireNonNull(emergencyId);
        return springDataRepository.existsByEmergencyId(emergencyId.value());
    }

    @Override
    public void createReserved(
            String reservationId,
            EmergencyId emergencyId,
            HospitalId hospitalId,
            long reservedAtTicks
    ) {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(emergencyId);
        Objects.requireNonNull(hospitalId);
        springDataRepository.saveAndFlush(new HospitalReservationJpaEntity(
                reservationId,
                emergencyId.value(),
                hospitalId.value(),
                "RESERVED",
                reservedAtTicks,
                null,
                null
        ));
    }
}
