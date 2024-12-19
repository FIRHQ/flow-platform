package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.flow.business.ListFlows;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ListFlowsImpl implements ListFlows {

    private final FlowRepo flowRepo;

    private final RequestContextHolder requestContextHolder;

    @Override
    public List<Flow> invoke(@Nullable Long parentId, PageRequest pageRequest) {
        if (parentId == null) {
            parentId = Flow.ROOT_ID;
        }

        return flowRepo.findAllByParentIdAndUserIdOrderByCreatedAt(
                parentId,
                requestContextHolder.getUserId(),
                pageRequest
        );
    }
}
