package com.flowci.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public final class ParallelStepNode extends Node {

    private Map<String, FlowNode> parallel = new LinkedHashMap<>();

    public ParallelStepNode(String name, Node parent) {
        super(name, parent);
    }
}