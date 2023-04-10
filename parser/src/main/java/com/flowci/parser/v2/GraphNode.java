package com.flowci.parser.v2;

import com.flowci.domain.node.Node;
import com.flowci.domain.node.StepNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GraphNode {

    private final List<GraphNode> parents = new LinkedList<>();

    private final Node node;

    private final List<GraphNode> children = new LinkedList<>();

    public String getName() {
        return node.getName();
    }

    public String getPath() {
        return node.getPath().getPathInStr();
    }
}
