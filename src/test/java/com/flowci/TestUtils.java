package com.flowci;

import org.instancio.Instancio;
import org.instancio.InstancioApi;

import java.time.Instant;

import static org.instancio.Select.all;

public abstract class TestUtils {

    public static <T> InstancioApi<T> newDummyInstance(Class<T> clazz) {
        return Instancio.of(clazz).ignore(all(Instant.class));
    }
}
