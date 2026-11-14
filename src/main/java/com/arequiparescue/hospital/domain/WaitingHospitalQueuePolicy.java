package com.arequiparescue.hospital.domain;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Política de dominio pura para ordenar estrictamente en FIFO la cola {@code WAITING_HOSPITAL}
 * según {@code hospital_queue_entered_at_t ASC, id ASC} sin aging ni pérdida de posición
 * (HU-10, RN-HOS-010, RN-HOS-011, CA-HOS-08, CA-HOS-09).
 */
public final class WaitingHospitalQueuePolicy {

    private static final Comparator<Emergency> FIFO_COMPARATOR = Comparator
            .comparingLong((Emergency e) -> e.hospitalQueueEnteredAt() != null
                    ? e.hospitalQueueEnteredAt().ticks()
                    : e.registeredAt().ticks())
            .thenComparing(e -> e.id().value());

    public List<Emergency> sortFifo(List<Emergency> waitingEmergencies) {
        Objects.requireNonNull(waitingEmergencies, "La lista de emergencias en espera hospitalaria no puede ser nula.");
        return waitingEmergencies.stream()
                .filter(e -> e.state() == EmergencyState.WAITING_HOSPITAL)
                .sorted(FIFO_COMPARATOR)
                .toList();
    }
}
