package com.flowci.flow.business.impl;

import com.flowci.common.model.Variables;
import com.flowci.flow.business.InitRootGroup;
import com.flowci.flow.model.Flow;
import com.flowci.flow.repo.FlowRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class InitRootGroupImpl implements InitRootGroup {

    private final FlowRepo flowRepo;

    @Override
    public void invoke() {
        var optional = flowRepo.findById(Flow.ROOT_ID);
        if (optional.isPresent()) {
            log.info("Root flow group '{}' already exists", Flow.ROOT_NAME);
            return;
        }

        var root = new Flow();
        root.setType(Flow.Type.GROUP);
        root.setName(Flow.ROOT_NAME);
        root.setParentId(Flow.ROOT_ID);
        root.setVariables(Variables.EMPTY);
        root.setCreatedBy("system");
        root.setUpdatedBy("system");
        flowRepo.save(root);
        log.info("Root flow group '{}' is created", root.getName());
    }
}
