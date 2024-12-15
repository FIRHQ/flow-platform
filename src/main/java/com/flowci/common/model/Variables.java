package com.flowci.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.common.ObjectMapperFactory;
import jakarta.persistence.Converter;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent variables by hashmap
 */
@Getter
public final class Variables extends HashMap<String, String> implements Serializable {

    public static final Variables EMPTY = new Variables(0);

    public Variables(int initialCapacity) {
        super(initialCapacity);
    }

    public Variables() {
        super(5);
    }

    public Variables(Map<String, String> data) {
        this.putAll(data);
    }

    @Converter(autoApply = true)
    public static class AttributeConverter implements jakarta.persistence.AttributeConverter<Variables, String> {

        private static final TypeReference<Map<String, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
        };

        @Override
        public String convertToDatabaseColumn(Variables attribute) {
            try {
                return ObjectMapperFactory.instance().writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to convert variables to JSON", e);
            }
        }

        @Override
        public Variables convertToEntityAttribute(String content) {
            try {
                var data = ObjectMapperFactory.instance().readValue(content, MAP_TYPE_REFERENCE);
                return new Variables(data);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to parse variables content", e);
            }
        }
    }
}
