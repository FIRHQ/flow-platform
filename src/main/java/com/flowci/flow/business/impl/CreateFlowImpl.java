package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchTemplateContent;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowYaml;
import com.flowci.flow.repo.FlowRepo;
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

    private final FetchTemplateContent fetchTemplateContent;

    private final RequestContextHolder requestContextHolder;

    @Override
    @Transactional
    public Flow invoke(CreateFlowParam param) {
        var flow = flowRepo.save(toObject(param));
        flowYamlRepo.save(toObject(flow, param));
        log.info("Created flow: {} by user {}", flow.getName(), flow.getCreatedBy());
        return flow;
    }

    private Flow toObject(CreateFlowParam param) {
        var flow = new Flow();
        flow.setName(param.name());
        flow.setCreatedBy(requestContextHolder.getUser());
        flow.setUpdatedBy(requestContextHolder.getUser());

        if (param.rootId() != null) {
            flow.setParentId(param.rootId());
        }

        return flow;
    }

    private FlowYaml toObject(Flow flow, CreateFlowParam param) {
        var flowYaml = new FlowYaml();
        flowYaml.setId(flow.getId());
        flowYaml.setCreatedBy(flow.getCreatedBy());
        flowYaml.setUpdatedBy(flow.getUpdatedBy());

        if (!param.isBlank()) {
            flowYaml.setYaml(fetchTemplateContent.invoke(param.template()));
        }

        return flowYaml;
    }
}
