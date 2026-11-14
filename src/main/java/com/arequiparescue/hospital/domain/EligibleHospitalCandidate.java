package com.arequiparescue.hospital.domain;

import com.arequiparescue.routing.domain.NodeId;

import java.util.List;

/**
 * Candidato hospitalario elegible para traslado tras confirmar la recogida en el incidente
 * (HU-09, HU-10, RN-HOS-005, RN-HOS-006).
 */
public record EligibleHospitalCandidate(
        Hospital hospital,
        int routeCost,
        List<NodeId> routePath
) {
}
