package com.flowci.build.business.impl;

import com.flowci.build.business.CreateBuild;
import com.flowci.build.business.TriggerBuild;
import com.flowci.build.business.WaitForAgent;
import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class TriggerBuildImpl implements TriggerBuild {

    private final CreateBuild createBuild;
    private final WaitForAgent waitForAgent;

    @Override
    @Transactional
    public Build invoke(Long flowId, Build.Trigger trigger, @Nullable Variables inputs) {
        var build = createBuild.invoke(flowId, trigger, inputs);
        waitForAgent.invoke(build.getId());
        return build;
    }
}
