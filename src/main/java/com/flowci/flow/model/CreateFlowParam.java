package com.flowci.flow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.common.validator.ValidName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record CreateFlowParam(@ValidName(message = "invalid flow name")
                              @Schema(description = "flow name", example = "hello_world", requiredMode = REQUIRED)
                              String name,

                              @Nullable
                              @Schema(description = "template name", example = "maven", requiredMode = NOT_REQUIRED)
                              String template,

                              @Nullable
                              @Schema(description = "parent flow id", example = "110011", requiredMode = NOT_REQUIRED)
                              Long parent) {

    private static final String BLANK_TEMPLATE = "_blank_";

    @JsonIgnore
    public boolean isBlank() {
        return template == null || BLANK_TEMPLATE.equals(template);
    }
}
