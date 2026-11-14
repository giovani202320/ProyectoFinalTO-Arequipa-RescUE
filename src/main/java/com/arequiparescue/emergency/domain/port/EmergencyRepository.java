package com.arequiparescue.emergency.domain.port;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistir y consultar emergencias simuladas sin acoplar el dominio
 * a Spring Data JPA ni Hibernate (DEC-002, DEC-003, ADR-0001).
 */
public interface EmergencyRepository {

    Emergency save(Emergency emergency);

    Optional<Emergency> findById(EmergencyId id);

    List<Emergency> findAllOrderByRegisteredAtAndId();

    long nextSequenceNumber();
}
