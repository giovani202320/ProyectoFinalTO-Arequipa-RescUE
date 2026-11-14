package com.arequiparescue.ambulance.domain.port;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistir y consultar el catálogo de ambulancias sin acoplar el dominio
 * a Spring Data JPA ni Hibernate (DEC-002, DEC-003, ADR-0001, HU-03).
 */
public interface AmbulanceRepository {

    Ambulance save(Ambulance ambulance);

    Optional<Ambulance> findById(AmbulanceId id);

    Optional<Ambulance> findByIdForUpdate(AmbulanceId id);

    List<Ambulance> findAllOrderById();

    boolean existsById(AmbulanceId id);

    long nextSequenceNumber();
}
