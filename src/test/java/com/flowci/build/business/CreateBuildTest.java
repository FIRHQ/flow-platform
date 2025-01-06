package com.flowci.build.business;

import com.flowci.SpringTest;
import com.flowci.build.model.Build;
import com.flowci.build.model.BuildYaml;
import com.flowci.common.model.Variables;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowYaml;
import com.flowci.yaml.business.ParseYamlV2;
import com.flowci.yaml.model.FlowV2;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.flowci.TestUtils.newDummyInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CreateBuildTest extends SpringTest {

    @MockBean
    private FetchFlow fetchFlow;

    @MockBean
    private FetchFlowYamlContent fetchFlowYamlContent;

    @MockBean
    private ParseYamlV2 parseYamlV2;

    @Autowired
    private MockRepositoriesConfig mockRepositoriesConfig;

    @Autowired
    private CreateBuild createBuild;

    @Test
    void givenFlow_whenCreating_thenBuildIsCreated() {
        var mockFlow = newDummyInstance(Flow.class).create();
        when(fetchFlow.invoke(anyLong())).thenReturn(mockFlow);
        var mockFlowYaml = newDummyInstance(FlowYaml.class).create();
        when(fetchFlowYamlContent.invoke(anyLong())).thenReturn(mockFlowYaml.getYaml());
        when(parseYamlV2.invoke(anyString())).thenReturn(Instancio.of(FlowV2.class).create());

        var mockBuildRepo = mockRepositoriesConfig.getBuildRepo();
        var buildCaptor = ArgumentCaptor.forClass(Build.class);
        when(mockBuildRepo.save(buildCaptor.capture()))
                .thenAnswer(opt -> opt.getArgument(0));

        var mockBuildYamlRepo = mockRepositoriesConfig.getBuildYamlRepo();
        var buildYamlCaptor = ArgumentCaptor.forClass(BuildYaml.class);
        when(mockBuildYamlRepo.save(buildYamlCaptor.capture()))
                .thenAnswer(opt -> opt.getArgument(0));

        var inputs = new Variables();
        inputs.put("v1", "hello");
        inputs.put("v2", "world");
        createBuild.invoke(1L, Build.Trigger.API, inputs);

        var build = buildCaptor.getValue();
        assertEquals(mockFlow.getId(), build.getFlowId());
        assertEquals(Build.Trigger.API, build.getTrigger());

        var buildYaml = buildYamlCaptor.getValue();
        assertEquals("hello", buildYaml.getVariables().get("v1"));
        assertEquals("world", buildYaml.getVariables().get("v2"));
        assertEquals(mockFlowYaml.getYaml(), buildYaml.getYaml());
    }
}
