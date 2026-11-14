package com.arequiparescue.hospital.domain.port;

import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistir y consultar el catálogo de hospitales sin acoplar el dominio
 * a Spring Data JPA ni Hibernate (DEC-002, DEC-003, ADR-0001, HU-03).
 */
public interface HospitalRepository {

    Hospital save(Hospital hospital);

    Optional<Hospital> findById(HospitalId id);

    Optional<Hospital> findByIdForUpdate(HospitalId id);

    List<Hospital> findAllOrderById();

    boolean existsById(HospitalId id);

    long nextSequenceNumber();
}
