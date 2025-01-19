package com.flowci.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }
}
