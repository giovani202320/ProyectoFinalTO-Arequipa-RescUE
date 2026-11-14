package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.routing.domain.NodeId;

import java.util.stream.Collectors;

/**
 * DTO de presentación para una ambulancia elegible evaluada en HU-05.
 */
public record EligibleAmbulanceView(
        String id,
        String equipmentTypeCode,
        String currentNodeId,
        String stateCode,
        int routeCost,
        String routePathFormatted,
        boolean supplementalSpecialized,
        boolean topRecommended
) {

    public static EligibleAmbulanceView fromDomain(EligibleAmbulanceCandidate candidate, boolean topRecommended) {
        String formattedPath = candidate.routePath().stream()
                .map(NodeId::value)
                .collect(Collectors.joining(" -> "));
        return new EligibleAmbulanceView(
                candidate.ambulance().id().value(),
                candidate.ambulance().equipmentType().name(),
                candidate.ambulance().currentNode().value(),
                candidate.ambulance().state().name(),
                candidate.routeCost(),
                formattedPath,
                candidate.supplementalSpecialized(),
                topRecommended
        );
    }
}
