package com.flowci.flow.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.common.model.EntityBase;
import lombok.Data;

@Data
public class GitLink {

    private String defaultBranch = "main";

    public static class Converter extends EntityBase.ObjectAttributeConverter<GitLink> {

        private static final TypeReference<GitLink> TYPE_REFERENCE = new TypeReference<>() {
        };

        @Override
        public TypeReference<GitLink> typeReference() {
            return TYPE_REFERENCE;
        }
    }
}
