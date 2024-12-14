package com.flowci.common.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class EntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof EntityBase e) {
            e.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
            e.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof EntityBase e) {
            e.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
