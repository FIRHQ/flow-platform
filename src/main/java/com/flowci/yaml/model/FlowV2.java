package com.flowci.yaml.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Setter
@Getter
public class FlowV2 extends BaseV2 {

    /**
     * List of agent tags
     */
    private Set<String> agents;

    private List<StepV2> steps;
}
