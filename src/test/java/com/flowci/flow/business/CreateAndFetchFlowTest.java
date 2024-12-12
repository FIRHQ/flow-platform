package com.flowci.flow.business;

import com.flowci.SpringTestWithDB;
import com.flowci.flow.model.CreateFlowParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CreateAndFetchFlowTest extends SpringTestWithDB {

    @Autowired
    private CreateFlow createFlow;

    @Autowired
    private FetchFlow fetchFlow;

    @Autowired
    private FetchFlowYamlContent fetchFlowYamlContent;

    @MockBean
    private FetchTemplateContent fetchTemplateContent;

    @BeforeEach
    void mockFetchTemplateContent() {
        when(fetchTemplateContent.invoke(anyString()))
                .thenReturn("helloworld yaml");
    }

    @Test
    void whenCreateFlowWithTemplate_thenFlowCreatedWithTemplateYamlContent() {
        var flow = createFlow.invoke(new CreateFlowParam("test flow", "helloworld", null));

        var f = fetchFlow.invoke(flow.getId());
        assertNotNull(f);

        var yaml = fetchFlowYamlContent.invoke(flow.getId());
        assertEquals("helloworld yaml", yaml);
        assertNotNull(f.getCreatedBy());
        assertNotNull(f.getUpdatedBy());
    }

    @Test
    void whenCreateFlowWithDuplicatedName_thenThrowException() {
        createFlow.invoke(new CreateFlowParam("flow A", null, null));

        assertThrows(DataIntegrityViolationException.class, () ->
                createFlow.invoke(new CreateFlowParam("flow A", null, null)));
    }
}
