package com.flowci.build.business.impl;

import com.flowci.build.business.CreateNewBuild;
import com.flowci.build.business.TriggerBuild;
import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TriggerBuildImpl implements TriggerBuild {

    private final CreateNewBuild createNewBuild;

    @Override
    public Build invoke(Long flowId, Build.Trigger trigger, Variables inputs) {
        var build = createNewBuild.invoke(flowId, trigger, inputs);

        // put to queue, and wait

        return null;
    }
}
