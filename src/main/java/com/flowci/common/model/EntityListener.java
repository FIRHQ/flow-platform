package com.flowci.common.model;

import com.flowci.common.TimeUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class EntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof EntityBase e) {
            e.createdAt = TimeUtils.now();
            e.updatedAt = TimeUtils.now();
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof EntityBase e) {
            e.updatedAt = TimeUtils.now();
        }
    }
}
