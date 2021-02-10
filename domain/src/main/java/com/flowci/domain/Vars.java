/*
 * Copyright 2019 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flowci.util.ObjectsHelper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = Vars.JSON_TYPE_FIELD)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StringVars.class, name = Vars.JSON_STRING_TYPE),
        @JsonSubTypes.Type(value = TypedVars.class, name = Vars.JSON_TYPED_TYPE)
})
public abstract class Vars<V> extends LinkedHashMap<String, V> implements Serializable {

    public static final String JSON_TYPE_FIELD = "_TYPE_";

    public static final String JSON_STRING_TYPE = "_string_";

    public static final String JSON_TYPED_TYPE = "_typed_";

    Vars() {
        super();
    }

    Vars(int size) {
        super(size);
    }

    public V get(String key, V defaultValue) {
        V value = get(key);
        return Objects.isNull(value) ? defaultValue : value;
    }

    public Vars<V> putAndReturn(String k, V v) {
        put(k, v);
        return this;
    }

    public Vars<V> merge(Vars<V> other) {
        return merge(other, true);
    }

    public Vars<V> merge(Vars<V> other, boolean overwrite) {
        if (!ObjectsHelper.hasCollection(other)) {
            return this;
        }

        for (Map.Entry<String, V> entry : other.entrySet()) {
            String key = entry.getKey();

            if (this.containsKey(key) && !overwrite) {
                continue;
            }

            put(key, entry.getValue());
        }

        return this;
    }

    /**
     * Convert vars to key=value list
     */
    public abstract List<String> toList();

    void merge(Map<String, V> vars) {
        if (Objects.isNull(vars)) {
            return;
        }

        for (Map.Entry<String, V> entry : vars.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
