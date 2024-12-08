package com.flowci.flow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YamlTemplate(@JsonProperty("title") String title,
                           @JsonProperty("desc") String desc,
                           @JsonProperty("url") String sourceUrl,
                           @JsonProperty(value = "default", defaultValue = "false") boolean isDefault) {
}
