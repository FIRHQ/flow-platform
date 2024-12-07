package com.flowci.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.common.ObjectMapperFactory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
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

    @Converter
    public abstract static class ObjectAttributeConverter<T> implements AttributeConverter<T, String> {

        public abstract TypeReference<T> typeReference();

        @Override
        public String convertToDatabaseColumn(T obj) {
            try {
                if (obj == null) {
                    return null;
                }
                return ObjectMapperFactory.instance().writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to convert object to JSON", e);
            }
        }

        @Override
        public T convertToEntityAttribute(String json) {
            try {
                if (json == null) {
                    return null;
                }
                return ObjectMapperFactory.instance().readValue(json, typeReference());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to parse object json content", e);
            }
        }
    }
}
