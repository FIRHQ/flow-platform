package com.flowci.tree;

import com.flowci.domain.LocalTask;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class FlowNode extends ParentNode {

    /**
     * Agent tags to set node running on which agent
     */
    private Selector selector;

    /**
     * Notification list that run locally
     */
    private List<LocalTask> notifications = new LinkedList<>();

    public FlowNode(String name) {
        super(name, null);
    }
}
