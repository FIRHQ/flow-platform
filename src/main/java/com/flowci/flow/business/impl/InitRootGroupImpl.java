package com.flowci.flow.business.impl;

import com.flowci.common.model.Variables;
import com.flowci.flow.business.InitRootGroup;
import com.flowci.flow.model.Group;
import com.flowci.flow.repo.GroupRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class InitRootGroupImpl implements InitRootGroup {

    private final GroupRepo groupRepo;

    @Override
    public void invoke() {
        var optional = groupRepo.findById(Group.ROOT_ID);
        if (optional.isPresent()) {
            log.info("Root group '{}' already exists", Group.ROOT_NAME);
            return;
        }

        var root = new Group();
        root.setName(Group.ROOT_NAME);
        root.setParentId(Group.ROOT_ID);
        root.setVariables(Variables.EMPTY);
        root.setCreatedBy("system");
        root.setUpdatedBy("system");
        groupRepo.save(root);
        log.info("Root group '{}' is created", root.getName());
    }
}
