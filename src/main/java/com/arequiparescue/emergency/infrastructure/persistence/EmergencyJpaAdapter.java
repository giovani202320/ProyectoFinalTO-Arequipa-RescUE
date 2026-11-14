package com.arequiparescue.emergency.infrastructure.persistence;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA que implementa el puerto de dominio {@link EmergencyRepository}
 * mediante traducción explícita entre modelos y asignación de secuencia bajo bloqueo
 * pesimista {@code SELECT ... FOR UPDATE} en InnoDB (DEC-002, DEC-003, ADR-0001, ADR-0002).
 */
@Repository
public class EmergencyJpaAdapter implements EmergencyRepository {

    public static final String EMERGENCY_SEQUENCE_NAME = "EMERGENCY";

    private final SpringDataEmergencyRepository springDataEmergencyRepository;
    private final SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    public EmergencyJpaAdapter(
            SpringDataEmergencyRepository springDataEmergencyRepository,
            SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository
    ) {
        this.springDataEmergencyRepository = springDataEmergencyRepository;
        this.springDataEmergencySequenceRepository = springDataEmergencySequenceRepository;
    }

    @Override
    @Transactional
    public Emergency save(Emergency emergency) {
        Objects.requireNonNull(emergency, "La emergencia a persistir no puede ser nula.");
        EmergencyJpaEntity entity = EmergencyPersistenceMapper.toJpaEntity(emergency);
        EmergencyJpaEntity saved = springDataEmergencyRepository.saveAndFlush(entity);
        return EmergencyPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Emergency> findById(EmergencyId id) {
        Objects.requireNonNull(id, "El identificador de la emergencia a consultar no puede ser nulo.");
        return springDataEmergencyRepository
                .findById(id.value())
                .map(EmergencyPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Emergency> findAllOrderByRegisteredAtAndId() {
        return springDataEmergencyRepository
                .findAllByOrderByRegisteredAtTicksAscIdAsc()
                .stream()
                .map(EmergencyPersistenceMapper::toDomain)
                .toList();
    }

    /**
     * Adquiere bloqueo pesimista de fila ({@code SELECT ... FOR UPDATE}) sobre la fila
     * {@code EMERGENCY} en {@code emergency_sequences}, consume el valor actual e incrementa
     * atómicamente {@code next_val} dentro de la transacción activa, impidiendo que dos
     * registros concurrentes obtengan el mismo identificador {@code EMG-XXXXX} (ADR-0002).
     */
    @Override
    @Transactional
    public long nextSequenceNumber() {
        EmergencySequenceJpaEntity sequence = springDataEmergencySequenceRepository
                .findByNameForUpdate(EMERGENCY_SEQUENCE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la secuencia inicial '" + EMERGENCY_SEQUENCE_NAME + "' en emergency_sequences."));
        long current = sequence.getNextVal();
        sequence.setNextVal(Math.addExact(current, 1L));
        springDataEmergencySequenceRepository.saveAndFlush(sequence);
        return current;
    }
}
