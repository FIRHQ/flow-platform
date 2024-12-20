package com.flowci.yaml.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class FlowV2 extends BaseV2 {

    /**
     * List of agent tags
     */
    private List<String> agents;

    private List<StepV2> steps;
}
