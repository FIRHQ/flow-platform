package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.ListFlows;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ListFlowsImpl implements ListFlows {

    private final FlowRepo flowRepo;

    private final FetchFlow fetchFlow;

    private final RequestContextHolder requestContextHolder;

    @Override
    public List<Flow> invoke(@Nullable Long parentId, PageRequest pageRequest) {
        if (parentId == null) {
            parentId = Flow.ROOT_ID;
        }

        var list = flowRepo.findAllByParentIdAndUserIdOrderByCreatedAt(
                parentId,
                requestContextHolder.getUserId(),
                pageRequest
        );

        var r = new ArrayList<Flow>(list.size() + 1);
        r.add(fetchFlow.invoke(Flow.ROOT_ID));
        r.addAll(list);
        return r;
    }
}
