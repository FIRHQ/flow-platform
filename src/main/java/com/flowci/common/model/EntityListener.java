package com.flowci.common.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

public class EntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof EntityBase e) {
            e.createdAt = Instant.now();
            e.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof EntityBase e) {
            e.updatedAt = Instant.now();
        }
    }
}
