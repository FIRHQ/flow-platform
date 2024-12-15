package com.flowci.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class TimeUtils {

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
