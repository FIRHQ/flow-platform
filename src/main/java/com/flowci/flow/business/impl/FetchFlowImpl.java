package com.flowci.flow.business.impl;

import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Slf4j
@Component
@AllArgsConstructor
public class FetchFlowImpl implements FetchFlow {

    private final FlowRepo flowRepo;

    @Override
    public Flow invoke(Long id) {
        var optional = flowRepo.findById(id);
        if (optional.isEmpty()) {
            throw new NotAvailableException(format("flow %s not found", id));
        }
        return optional.get();
    }

    @Override
    public Flow invoke(String name) {
        var optional = flowRepo.findByName(name);
        if (optional.isEmpty()) {
            throw new NotAvailableException(format("flow %s not found", name));
        }
        return optional.get();
    }
}
