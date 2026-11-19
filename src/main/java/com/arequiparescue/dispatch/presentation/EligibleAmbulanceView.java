package com.arequiparescue.dispatch.presentation;

import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.routing.domain.NodeId;

import java.util.List;
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
        List<String> routePathNodes,
        boolean supplementalSpecialized,
        boolean topRecommended
) {

    public static EligibleAmbulanceView fromDomain(EligibleAmbulanceCandidate candidate, boolean topRecommended) {
        List<String> pathNodes = candidate.routePath().stream()
                .map(NodeId::value)
                .toList();
        String formattedPath = String.join(" -> ", pathNodes);
        return new EligibleAmbulanceView(
                candidate.ambulance().id().value(),
                candidate.ambulance().equipmentType().name(),
                candidate.ambulance().currentNode().value(),
                candidate.ambulance().state().name(),
                candidate.routeCost(),
                formattedPath,
                pathNodes,
                candidate.supplementalSpecialized(),
                topRecommended
        );
    }
}
