package com.flowci.flow.business.impl;

import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.repo.FlowYamlRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Slf4j
@Component
@AllArgsConstructor
public class FetchFlowYamlContentImpl implements FetchFlowYamlContent {

    private final FlowYamlRepo flowYamlRepo;

    @Override
    public String invoke(Long id) {
        var optional = flowYamlRepo.findById(id);
        if (optional.isEmpty()) {
            throw new NotAvailableException(format("flow %s not found", id));
        }
        return optional.get().getYaml();
    }
}
