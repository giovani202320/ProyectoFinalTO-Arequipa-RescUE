package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code emergency_sequences}, utilizada para generar identificadores
 * canónicos {@code EMG-XXXXX} bajo bloqueo pesimista {@code SELECT ... FOR UPDATE} (ADR-0001, ADR-0002).
 */
@Entity
@Table(name = "emergency_sequences")
public class EmergencySequenceJpaEntity {

    @Id
    @Column(name = "sequence_name", length = 32, nullable = false)
    private String sequenceName;

    @Column(name = "next_val", nullable = false)
    private long nextVal;

    protected EmergencySequenceJpaEntity() {
        // Requerido por JPA
    }

    public EmergencySequenceJpaEntity(String sequenceName, long nextVal) {
        this.sequenceName = sequenceName;
        this.nextVal = nextVal;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public long getNextVal() {
        return nextVal;
    }

    public void setNextVal(long nextVal) {
        this.nextVal = nextVal;
    }
}
