package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.common.model.Variables;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowYaml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CreateAndFetchFlowTest extends SpringTest {

    @Autowired
    private MockRepositoriesConfig mockRepositoriesConfig;

    @Autowired
    private CreateFlow createFlow;

    @MockBean
    private FetchTemplateContent fetchTemplateContent;

    @BeforeEach
    void mockFetchTemplateContent() {
        when(fetchTemplateContent.invoke(anyString())).thenReturn("helloworld yaml");
    }

    @Test
    void whenCreateFlowWithTemplate_thenFlowCreatedWithTemplateYamlContent() {
        var flowRepoMock = mockRepositoriesConfig.getFlowRepo();
        var flowArgCaptor = ArgumentCaptor.forClass(Flow.class);
        when(flowRepoMock.save(flowArgCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        var flowYamlRepoMock = mockRepositoriesConfig.getFlowYamlRepo();
        var flowYamlArgCaptor = ArgumentCaptor.forClass(FlowYaml.class);
        when(flowYamlRepoMock.save(flowYamlArgCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        var param = new CreateFlowParam("test flow", "helloworld", null);
        createFlow.invoke(param);

        // verify saved flow
        verify(flowRepoMock, times(1)).save(flowArgCaptor.capture());

        var savedFlow = flowArgCaptor.getValue();
        assertEquals(param.name(), savedFlow.getName());
        assertEquals(Flow.Type.FLOW, savedFlow.getType());
        assertEquals(Variables.EMPTY, savedFlow.getVariables());
        assertNotNull(savedFlow.getCreatedBy());
        assertNotNull(savedFlow.getUpdatedBy());

        // verify saved flow yaml
        verify(flowYamlRepoMock, times(1)).save(flowYamlArgCaptor.capture());

        var savedFlowYaml = flowYamlArgCaptor.getValue();
        assertEquals("helloworld yaml", savedFlowYaml.getYaml());
        assertEquals(savedFlow.getCreatedBy(), savedFlowYaml.getCreatedBy());
        assertEquals(savedFlow.getUpdatedBy(), savedFlowYaml.getUpdatedBy());
    }

    @Test
    void whenCreateFlowWithDuplicatedName_thenThrowException() {
        var flowRepoMock = mockRepositoriesConfig.getFlowRepo();
        when(flowRepoMock.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DataIntegrityViolationException.class, () ->
                createFlow.invoke(new CreateFlowParam("flow A", null, null)));
    }
}
