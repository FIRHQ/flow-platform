package com.flowci.yaml;

import com.flowci.SpringTest;
import com.flowci.yaml.business.ParseYamlV2;
import com.flowci.yaml.exception.InvalidYamlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class ParseYamlV2Test extends SpringTest {

    @Autowired
    private ParseYamlV2 parseYamlV2;

    @Test
    void givenYaml_whenParsing_thenReturnFlowObject() {
        var content = getResourceAsString("yaml/v2_success.yaml");
        var flowV2 = parseYamlV2.invoke(content);
        assertNotNull(flowV2);

        // verify agents
        var agents = flowV2.getAgents();
        assertEquals(2, agents.size());
        assertTrue(agents.contains("k8s"));
        assertTrue(agents.contains("java"));

        // verify condition
        var expCondition = "return $FLOWCI_GIT_BRANCH == \"develop\" || $FLOWCI_GIT_BRANCH == \"master\";";
        assertEquals(expCondition, flowV2.getCondition());

        // verify variables
        var vars = flowV2.getVariables();
        assertEquals(2, vars.size());
        assertEquals("/home/flowci", vars.get("FLOW_WORKSPACE"));
        assertEquals("2.0", vars.get("FLOW_VERSION"));

        // verify steps
        var steps = flowV2.getSteps();
        assertEquals(2, steps.size());

        // verify step 1
        var step1 = steps.getFirst();
        assertEquals("step_1", step1.getName());
        assertEquals(3600, step1.getTimeout());
        assertEquals(true, step1.getAllowFailure());
        assertEquals(3, step1.getVariables().size());
        assertEquals("/home/step/1", step1.getVariables().get("FLOW_WORKSPACE"));
        assertEquals("2.1", step1.getVariables().get("FLOW_VERSION"));
        assertEquals("/k8s/test", step1.getVariables().get("K8S_PATH"));

        var step1Commands = step1.getCommands();
        assertEquals("echo \"step 1 on bash\"", step1Commands.getFirst().getBash());
        assertEquals("echo \"step 1 on powershell\"", step1Commands.getFirst().getPwsh());

        // verify step 1 docker that is inherited from flow
        var step1Docker = step1.getDocker();
        assertNotNull(step1Docker);
        assertEquals("alpine:3", step1Docker.getImage());
        assertEquals("8080:8080", step1Docker.getPorts().getFirst());

        // verify step 2
        var step2 = steps.get(1);
        assertEquals("step_2", step2.getName());
        assertEquals(1800, step2.getTimeout());
        assertEquals("step_1", step2.getDependsOn().getFirst());
        assertEquals(false, step2.getAllowFailure());

        var step2Commands = step2.getCommands();
        assertEquals("echo \"step 2 on bash\"", step2Commands.getFirst().getBash());
        assertEquals("echo \"step 2 on powershell\"", step2Commands.getFirst().getPwsh());

        // verify step 2 variables that is inherited from flow
        var step2Vars = step2.getVariables();
        assertEquals(2, step2Vars.size());
        assertEquals("/home/flowci", step2Vars.get("FLOW_WORKSPACE"));
        assertEquals("2.0", step2Vars.get("FLOW_VERSION"));

        // verify step 2 docker that is overwritten the flow docker
        var step2Docker = step2.getDocker();
        assertEquals("ubuntu:24.04", step2Docker.getImage());
        assertEquals("6400:6400", step2Docker.getPorts().getFirst());
        assertEquals("/bin/sh", step2Docker.getEntrypoint().getFirst());
        assertEquals("host", step2Docker.getNetwork());
    }

    @ParameterizedTest
    @CsvSource({
            "v2_step_name_invalid.yaml,step name 'step 1' is invalid",
            "v2_step_name_duplicate.yaml,step name 'step_1' already exists",
            "v2_step_depends_on_not_found.yaml,depends on 'step_not_there' is not found",
            "v2_commands_missing.yaml,at least one command under step 'step_1' is required",
            "v2_commands_without_script.yaml,bash or powershell is required",
            "v2_circular_dependency_on_all_depends.yaml,circular dependency found",
            "v2_circular_dependency_with_separate_step.yaml,circular dependency found",
    })
    void givenInvalidYaml_whenParsing_thenThrowException(String yamlFile, String expectedMsg) {
        var content = getResourceAsString("yaml/" + yamlFile);
        var exception = assertThrows(InvalidYamlException.class, () -> parseYamlV2.invoke(content));
        assertEquals(expectedMsg, exception.getMessage());
    }
}
