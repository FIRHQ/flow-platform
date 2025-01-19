package com.flowci.flow.business;

import com.flowci.flow.model.Flow;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ListFlows {
    List<Flow> invoke(@Nullable Long parentId);
}
