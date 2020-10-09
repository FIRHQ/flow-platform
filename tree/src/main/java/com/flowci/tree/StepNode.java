package com.flowci.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class StepNode extends Node {

    public static final boolean ALLOW_FAILURE_DEFAULT = false;

    /**
     * Node before groovy script;
     */
    private String condition;

    /**
     * Node execute script, can be null
     */
    private String script;

    /**
     * Plugin name
     */
    private String plugin;

    private Set<String> exports = new HashSet<>(0);

    private int order;

    /**
     * Is allow failure
     */
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    public StepNode(String name, Node parent) {
        super(name, parent);
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return !Strings.isNullOrEmpty(plugin);
    }

    @JsonIgnore
    public boolean hasCondition() {
        return !Strings.isNullOrEmpty(condition);
    }

    @JsonIgnore
    public boolean hasScript() {
        return !Strings.isNullOrEmpty(script);
    }

    @JsonIgnore
    public boolean isRootStep() {
        return parent instanceof FlowNode;
    }
}
