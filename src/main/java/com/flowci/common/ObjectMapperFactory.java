package com.flowci.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public abstract class ObjectMapperFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DateTimeFormatter UTC_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    private static final JsonSerializer<Instant> INSTANT_JSON_SERIALIZER = new JsonSerializer<>() {
        @Override
        public void serialize(Instant instant, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String str = UTC_FORMAT.format(instant);
            gen.writeString(str);
        }
    };

    private static final JsonDeserializer<Instant> INSTANT_JSON_DESERIALIZER = new JsonDeserializer<>() {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Instant.from(UTC_FORMAT.parse(p.getText()));
        }
    };

    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var module = new SimpleModule();
        module.addSerializer(Instant.class, INSTANT_JSON_SERIALIZER);
        module.addDeserializer(Instant.class, INSTANT_JSON_DESERIALIZER);
        MAPPER.registerModule(module);
    }

    public static ObjectMapper instance() {
        return MAPPER;
    }
}
