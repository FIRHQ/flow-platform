package com.flowci.flow.model;

import jakarta.annotation.Nullable;

public record CreateFlowParam(String name,
                              @Nullable String template,
                              @Nullable Long rootId) {
}
