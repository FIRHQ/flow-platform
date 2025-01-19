package com.flowci.yaml.business.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flowci.common.validator.ValidName;
import com.flowci.yaml.business.ParseYamlV2;
import com.flowci.yaml.exception.InvalidYamlException;
import com.flowci.yaml.model.FlowV2;
import com.flowci.yaml.model.StepV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
public class ParseYamlV2Impl implements ParseYamlV2 {

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private static final ValidName.NameValidator nameValidator = new ValidName.NameValidator();

    static {
        objectMapper.findAndRegisterModules();
    }

    @Override
    public FlowV2 invoke(String yaml) {
        try {
            var flowV2 = objectMapper.readValue(yaml, FlowV2.class);
            for (var step : flowV2.getSteps()) {
                step.setParent(flowV2);
            }
            validateSteps(flowV2);
            buildGraph(flowV2);
            return flowV2;
        } catch (JsonProcessingException e) {
            log.error("invalid YAML configuration", e);
            throw new InvalidYamlException("invalid YAML configuration");
        }
    }

    private void validateSteps(FlowV2 flow) {
        var steps = flow.getSteps();
        if (CollectionUtils.isEmpty(steps)) {
            throw new InvalidYamlException("at least one step is required");
        }

        var stepNameSet = new HashSet<>(steps.size());
        for (var step : steps) {
            if (!stepNameSet.add(step.getName())) {
                throw new InvalidYamlException(format("step name '%s' already exists", step.getName()));
            }
        }

        for (var step : steps) {
            if (!nameValidator.isValid(step.getName(), null)) {
                throw new InvalidYamlException(format("step name '%s' is invalid", step.getName()));
            }

            if (!isEmpty(step.getDependsOn())) {
                for (var dependsOn : step.getDependsOn()) {
                    if (!stepNameSet.contains(dependsOn)) {
                        throw new InvalidYamlException(format("depends on '%s' is not found", dependsOn));
                    }
                }
            }

            validateCommands(step);
        }
    }

    private void validateCommands(StepV2 step) {
        var commands = step.getCommands();
        if (CollectionUtils.isEmpty(commands)) {
            throw new InvalidYamlException(format("at least one command under step '%s' is required", step.getName()));
        }

        for (var command : commands) {
            if (!hasText(command.getBash()) && !hasText(command.getPwsh())) {
                throw new InvalidYamlException("bash or powershell is required");
            }
        }
    }

    private void buildGraph(FlowV2 flow) {
        var steps = flow.getSteps();
        var map = new HashMap<String, StepV2>(steps.size());
        for (var step : steps) {
            map.put(step.getName(), step);
        }

        for (var step : steps) {
            if (step.getDependsOn() == null) {
                continue;
            }

            for (var dep : step.getDependsOn()) {
                var depStep = map.get(dep);
                depStep.getNext().add(step);
            }
        }

        for (var step : steps) {
            checkCircularDependencies(List.of(step), new HashSet<>());
        }
    }

    private void checkCircularDependencies(List<StepV2> steps, Set<StepV2> traversed) {
        for (var step : steps) {
            if (!traversed.add(step)) {
                throw new InvalidYamlException("circular dependency found");
            }

            traversed.add(step);
            checkCircularDependencies(step.getNext(), traversed);
        }
    }
}