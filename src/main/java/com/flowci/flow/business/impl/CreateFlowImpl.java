package com.flowci.flow.business.impl;

import com.flowci.common.RequestContextHolder;
import com.flowci.common.exception.DuplicateException;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchTemplateContent;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.flowci.common.exception.ExceptionUtils.tryConvertToBusinessException;

@Slf4j
@Component
@AllArgsConstructor
public class CreateFlowImpl implements CreateFlow {

    private final FlowRepo flowRepo;

    private final FetchTemplateContent fetchTemplateContent;

    private final RequestContextHolder requestContextHolder;

    @Override
    public Long invoke(CreateFlowParam param) {
        var flow = toObject(param);

        try {
            var saved = flowRepo.save(flow);
            log.info("Created flow: {} by user {}", flow.getName(), flow.getCreatedBy());
            return saved.getId();
        } catch (Throwable e) {
            throw tryConvertToBusinessException(e, DuplicateException.class, "dup");
        }
    }

    private Flow toObject(CreateFlowParam param) {
        var flow = new Flow();
        flow.setName(param.name());
        flow.setCreatedBy(requestContextHolder.getUser());
        flow.setUpdatedBy(requestContextHolder.getUser());

        if (param.rootId() != null) {
            flow.setParentId(param.rootId());
        }

        if (param.template() != null) {
            var templateYaml = fetchTemplateContent.invoke(param.template());
            flow.setYaml(templateYaml);
        }

        return flow;
    }
}
