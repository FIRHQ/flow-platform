package com.flowci.flow.business;

import com.flowci.flow.model.Flow;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface ListFlows {
    List<Flow> invoke(@Nullable Long parentId, PageRequest pageRequest);
}
