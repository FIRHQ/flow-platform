package com.flowci.build.business.impl;

import com.flowci.build.business.WaitForAgent;
import com.flowci.build.model.Build;
import com.flowci.build.repo.BuildRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class WaitForAgentImpl implements WaitForAgent {

    private final BuildRepo buildRepo;

    @Override
    public void invoke(Long buildId) {
        buildRepo.updateBuildStatusById(buildId, Build.Status.QUEUED);
    }
}
