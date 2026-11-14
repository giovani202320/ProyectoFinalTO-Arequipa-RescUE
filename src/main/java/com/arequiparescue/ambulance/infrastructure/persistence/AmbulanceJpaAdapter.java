package com.arequiparescue.ambulance.infrastructure.persistence;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA que implementa {@link AmbulanceRepository} (DEC-002, DEC-003, ADR-0001, ADR-0002).
 */
@Repository
public class AmbulanceJpaAdapter implements AmbulanceRepository {

    public static final String AMBULANCE_SEQUENCE_NAME = "AMBULANCE";

    private final SpringDataAmbulanceRepository ambulanceRepository;
    private final SpringDataEmergencySequenceRepository sequenceRepository;

    public AmbulanceJpaAdapter(
            SpringDataAmbulanceRepository ambulanceRepository,
            SpringDataEmergencySequenceRepository sequenceRepository
    ) {
        this.ambulanceRepository = ambulanceRepository;
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional
    public Ambulance save(Ambulance ambulance) {
        Objects.requireNonNull(ambulance, "La ambulancia a persistir no puede ser nula.");
        AmbulanceJpaEntity saved = ambulanceRepository.saveAndFlush(AmbulancePersistenceMapper.toJpaEntity(ambulance));
        return AmbulancePersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ambulance> findById(AmbulanceId id) {
        Objects.requireNonNull(id, "El identificador de la ambulancia no puede ser nulo.");
        return ambulanceRepository.findById(id.value()).map(AmbulancePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Ambulance> findByIdForUpdate(AmbulanceId id) {
        Objects.requireNonNull(id, "El identificador de la ambulancia no puede ser nulo.");
        return ambulanceRepository.findByIdForUpdate(id.value()).map(AmbulancePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ambulance> findAllOrderById() {
        return ambulanceRepository.findAllByOrderByIdAsc()
                .stream()
                .map(AmbulancePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(AmbulanceId id) {
        Objects.requireNonNull(id, "El identificador de la ambulancia no puede ser nulo.");
        return ambulanceRepository.existsById(id.value());
    }

    @Override
    @Transactional
    public long nextSequenceNumber() {
        EmergencySequenceJpaEntity sequence = sequenceRepository
                .findByNameForUpdate(AMBULANCE_SEQUENCE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la secuencia '" + AMBULANCE_SEQUENCE_NAME + "' en emergency_sequences."));
        long current = sequence.getNextVal();
        sequence.setNextVal(Math.addExact(current, 1L));
        sequenceRepository.saveAndFlush(sequence);
        return current;
    }
}
