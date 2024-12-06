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
public final class Variables implements Serializable {

    private final Map<String, String> data;

    public Variables() {
        this.data = new HashMap<>(5);
    }

    public Variables(Map<String, String> data) {
        this.data = data;
    }

    public String get(String key) {
        return data.get(key);
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public int size() {
        return data.size();
    }

    @Converter(autoApply = true)
    public static class AttributeConverter implements jakarta.persistence.AttributeConverter<Variables, String> {

        private static final TypeReference<Map<String, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
        };

        @Override
        public String convertToDatabaseColumn(Variables attribute) {
            try {
                return ObjectMapperFactory.instance().writeValueAsString(attribute.data);
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
