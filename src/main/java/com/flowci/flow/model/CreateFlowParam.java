package com.flowci.flow.model;

import com.flowci.common.validator.ValidName;
import jakarta.annotation.Nullable;

public record CreateFlowParam(@ValidName(message = "invalid flow name") String name,
                              @Nullable String template,
                              @Nullable Long rootId) {

    private static final String BLANK_TEMPLATE = "_blank_";

    public boolean isBlank() {
        return template == null || BLANK_TEMPLATE.equals(template);
    }
}
