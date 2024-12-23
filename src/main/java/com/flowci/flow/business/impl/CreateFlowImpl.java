package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchTemplateContent;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowUser;
import com.flowci.flow.model.FlowYaml;
import com.flowci.flow.repo.FlowRepo;
import com.flowci.flow.repo.FlowUserRepo;
import com.flowci.flow.repo.FlowYamlRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class CreateFlowImpl implements CreateFlow {

    private final FlowRepo flowRepo;
    private final FlowYamlRepo flowYamlRepo;
    private final FlowUserRepo flowUserRepo;
    private final FetchTemplateContent fetchTemplateContent;
    private final RequestContextHolder requestContextHolder;

    @Override
    @Transactional
    public Flow invoke(CreateFlowParam param) {
        var flow = flowRepo.save(toObject(param));
        flowYamlRepo.save(toObject(flow, param));
        flowUserRepo.save(toObject(flow));
        log.info("Created flow: {} by user {}", flow.getName(), flow.getCreatedBy());
        return flow;
    }

    private Flow toObject(CreateFlowParam param) {
        var flow = new Flow();
        flow.setName(param.name());
        flow.setType(Flow.Type.FLOW);
        flow.setVariables(Variables.EMPTY);
        flow.setParentId(param.parent() == null ? Flow.ROOT_ID : param.parent());
        flow.setCreatedBy(requestContextHolder.getUserId());
        flow.setUpdatedBy(requestContextHolder.getUserId());
        return flow;
    }

    private FlowYaml toObject(Flow flow, CreateFlowParam param) {
        var flowYaml = new FlowYaml();
        flowYaml.setId(flow.getId());
        flowYaml.setYaml("");
        flowYaml.setCreatedBy(flow.getCreatedBy());
        flowYaml.setUpdatedBy(flow.getUpdatedBy());

        if (!param.isBlank()) {
            flowYaml.setYaml(fetchTemplateContent.invoke(param.template()));
        }

        return flowYaml;
    }

    private FlowUser toObject(Flow flow) {
        var fu = new FlowUser();
        fu.setFlowId(flow.getId());
        fu.setUserId(flow.getCreatedBy());
        fu.setCreatedBy(flow.getCreatedBy());
        fu.setUpdatedBy(flow.getUpdatedBy());
        return fu;
    }
}
