package com.flowci.common.model;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@EntityListeners(EntityListener.class)
@MappedSuperclass
public abstract class EntityBase implements Serializable {

    protected Instant createdAt;

    protected String createdBy;

    protected Instant updatedAt;

    protected String updatedBy;
}
