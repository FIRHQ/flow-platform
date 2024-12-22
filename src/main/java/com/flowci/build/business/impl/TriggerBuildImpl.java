package com.flowci.build.business.impl;

import com.flowci.build.business.TriggerBuild;
import com.flowci.build.model.Build;
import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.BuildYamlRepo;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.FetchFlow;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TriggerBuildImpl implements TriggerBuild {

    private final FetchFlow fetchFlow;

    private final BuildRepo buildRepo;

    private final BuildYamlRepo buildYamlRepo;

    @Override
    public Build invoke(Long flowId, Build.Trigger trigger, Variables inputs) {
        return null;
    }
}
