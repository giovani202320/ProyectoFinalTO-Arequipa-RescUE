package com.arequiparescue.hospital.infrastructure.persistence;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.HospitalReservationStatus;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
                HospitalReservationStatus.RESERVED.name(),
                reservedAtTicks,
                null,
                null
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HospitalReservation> findById(String reservationId) {
        Objects.requireNonNull(reservationId);
        return springDataRepository.findById(reservationId).map(HospitalReservationJpaAdapter::toDomain);
    }

    @Override
    public Optional<HospitalReservation> findByIdForUpdate(String reservationId) {
        Objects.requireNonNull(reservationId);
        return springDataRepository.findByIdForUpdate(reservationId).map(HospitalReservationJpaAdapter::toDomain);
    }

    @Override
    public Optional<HospitalReservation> findByEmergencyIdForUpdate(EmergencyId emergencyId) {
        Objects.requireNonNull(emergencyId);
        return springDataRepository.findByEmergencyIdForUpdate(emergencyId.value())
                .map(HospitalReservationJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalReservation> findAllOrderByReservedAtAndId() {
        return springDataRepository.findAllByOrderByReservedAtTicksAscIdAsc().stream()
                .map(HospitalReservationJpaAdapter::toDomain)
                .toList();
    }

    @Override
    public HospitalReservation save(HospitalReservation reservation) {
        Objects.requireNonNull(reservation);
        HospitalReservationJpaEntity saved = springDataRepository.saveAndFlush(new HospitalReservationJpaEntity(
                reservation.id(),
                reservation.emergencyId().value(),
                reservation.hospitalId().value(),
                reservation.status().name(),
                reservation.reservedAt().ticks(),
                reservation.occupiedAt() != null ? reservation.occupiedAt().ticks() : null,
                reservation.dischargedAt() != null ? reservation.dischargedAt().ticks() : null
        ));
        return toDomain(saved);
    }

    private static HospitalReservation toDomain(HospitalReservationJpaEntity entity) {
        return HospitalReservation.reconstruct(
                entity.getId(),
                EmergencyId.of(entity.getEmergencyId()),
                HospitalId.of(entity.getHospitalId()),
                HospitalReservationStatus.valueOf(entity.getStatus()),
                LogicalTime.of(entity.getReservedAtTicks()),
                entity.getOccupiedAtTicks() != null ? LogicalTime.of(entity.getOccupiedAtTicks()) : null,
                entity.getDischargedAtTicks() != null ? LogicalTime.of(entity.getDischargedAtTicks()) : null
        );
    }
}
