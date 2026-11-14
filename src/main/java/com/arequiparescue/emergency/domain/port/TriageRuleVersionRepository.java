package com.arequiparescue.emergency.domain.port;

import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;

import java.util.Optional;

/**
 * Puerto de dominio para consultar versiones del catálogo de reglas de clasificación simulada
 * sin acoplar el dominio a detalles de infraestructura (DEC-002, DEC-003, DEC-016, ADR-0001).
 */
public interface TriageRuleVersionRepository {

    Optional<TriageRuleVersion> findActiveVersion();

    Optional<TriageRuleVersion> findById(TriageRuleVersionId id);
}
