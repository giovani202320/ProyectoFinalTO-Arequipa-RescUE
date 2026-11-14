package com.arequiparescue.emergency.domain.port;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistir y consultar emergencias simuladas sin acoplar el dominio
 * a Spring Data JPA ni Hibernate (DEC-002, DEC-003, ADR-0001).
 */
public interface EmergencyRepository {

    Emergency save(Emergency emergency);

    Optional<Emergency> findById(EmergencyId id);

    Optional<Emergency> findByIdForUpdate(EmergencyId id);

    List<Emergency> findAllOrderByRegisteredAtAndId();

    List<Emergency> findWaitingAmbulanceOrderByQueueEnteredAtAndId();

    List<Emergency> findWaitingHospitalOrderByHospitalQueueEnteredAtAndId();

    default List<Emergency> findByStateOrderByRegisteredAtAndId(EmergencyState state) {
        return List.of();
    }

    long nextSequenceNumber();
}
