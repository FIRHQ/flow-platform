package com.flowci.yaml.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class BaseV2 {

    private static final Integer DEFAULT_TIMEOUT = 1800;

    protected Map<String, String> variables = new LinkedHashMap<>();

    protected Integer timeout = DEFAULT_TIMEOUT; // timeout in seconds

    /**
     * Groovy script
     */
    protected String condition;

    protected DockerV2 docker;

    protected List<DockerV2> dockers;

    public String getCondition() {
        return condition.trim();
    }
}
