package com.arequiparescue.hospital.infrastructure.persistence;

import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA que implementa {@link HospitalRepository} (DEC-002, DEC-003, ADR-0001, ADR-0002).
 */
@Repository
public class HospitalJpaAdapter implements HospitalRepository {

    public static final String HOSPITAL_SEQUENCE_NAME = "HOSPITAL";

    private final SpringDataHospitalRepository hospitalRepository;
    private final SpringDataEmergencySequenceRepository sequenceRepository;

    public HospitalJpaAdapter(
            SpringDataHospitalRepository hospitalRepository,
            SpringDataEmergencySequenceRepository sequenceRepository
    ) {
        this.hospitalRepository = hospitalRepository;
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional
    public Hospital save(Hospital hospital) {
        Objects.requireNonNull(hospital, "El hospital a persistir no puede ser nulo.");
        HospitalJpaEntity saved = hospitalRepository.saveAndFlush(HospitalPersistenceMapper.toJpaEntity(hospital));
        return HospitalPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Hospital> findById(HospitalId id) {
        Objects.requireNonNull(id, "El identificador del hospital no puede ser nulo.");
        return hospitalRepository.findById(id.value()).map(HospitalPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Hospital> findByIdForUpdate(HospitalId id) {
        Objects.requireNonNull(id, "El identificador del hospital no puede ser nulo.");
        return hospitalRepository.findByIdForUpdate(id.value()).map(HospitalPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hospital> findAllOrderById() {
        return hospitalRepository.findAllByOrderByIdAsc()
                .stream()
                .map(HospitalPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(HospitalId id) {
        Objects.requireNonNull(id, "El identificador del hospital no puede ser nulo.");
        return hospitalRepository.existsById(id.value());
    }

    @Override
    @Transactional
    public long nextSequenceNumber() {
        EmergencySequenceJpaEntity sequence = sequenceRepository
                .findByNameForUpdate(HOSPITAL_SEQUENCE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la secuencia '" + HOSPITAL_SEQUENCE_NAME + "' en emergency_sequences."));
        long current = sequence.getNextVal();
        sequence.setNextVal(Math.addExact(current, 1L));
        sequenceRepository.saveAndFlush(sequence);
        return current;
    }
}
