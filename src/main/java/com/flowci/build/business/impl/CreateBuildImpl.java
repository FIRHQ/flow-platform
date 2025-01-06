package com.flowci.build.business.impl;

import com.flowci.build.business.CreateBuild;
import com.flowci.build.business.TriggerBuild;
import com.flowci.build.model.Build;
import com.flowci.build.model.BuildYaml;
import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.BuildYamlRepo;
import com.flowci.common.RequestContextHolder;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.model.Flow;
import com.flowci.yaml.business.ParseYamlV2;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
public class CreateBuildImpl implements CreateBuild {

    private final FetchFlow fetchFlow;
    private final FetchFlowYamlContent fetchFlowYamlContent;
    private final ParseYamlV2 parseYamlV2;
    private final BuildRepo buildRepo;
    private final BuildYamlRepo buildYamlRepo;
    private final RequestContextHolder requestContextHolder;

    @Override
    @Transactional
    public Build invoke(Long flowId, Build.Trigger trigger, Variables inputs) {
        var flow = fetchFlow.invoke(flowId);

        var yaml = fetchFlowYamlContent.invoke(flowId);
        var yamlObj = parseYamlV2.invoke(yaml);
        var agentTags = yamlObj.getAgents() == null
                ? Set.<String>of()
                : new HashSet<>(yamlObj.getAgents());

        var build = new Build();
        build.setFlowId(flow.getId());
        build.setTrigger(trigger);
        build.setStatus(Build.Status.CREATED);
        build.setAgentTags(agentTags.toArray(new String[0]));
        build.setCreatedBy(requestContextHolder.getUserId());
        build.setUpdatedBy(requestContextHolder.getUserId());
        buildRepo.save(build);

        var buildYaml = new BuildYaml();
        buildYaml.setId(build.getId());
        buildYaml.setVariables(toBuildVariables(flow, inputs));
        buildYaml.setYaml(yaml);
        buildYaml.setCreatedBy(build.getCreatedBy());
        buildYaml.setUpdatedBy(build.getUpdatedBy());
        buildYamlRepo.save(buildYaml);

        log.info("build {} is created for flow {} with trigger {}", build.getBuildAlias(), flowId, trigger);
        return build;
    }

    private Variables toBuildVariables(Flow flow, Variables inputs) {
        var variables = new Variables(flow.getVariables());
        variables.putAll(inputs); // inputs has top priority
        return variables;
    }
}
