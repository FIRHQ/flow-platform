package com.flowci.flow.business;

import com.flowci.SpringTestWithDB;
import com.flowci.common.exception.DuplicateException;
import com.flowci.flow.model.CreateFlowParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CreateAndFetchFlowTest extends SpringTestWithDB {

    @Autowired
    private CreateFlow createFlow;

    @Autowired
    private FetchFlow fetchFlow;

    @MockBean
    private FetchTemplateContent fetchTemplateContent;

    @BeforeEach
    void mockFetchTemplateContent() {
        when(fetchTemplateContent.invoke(anyString()))
                .thenReturn("helloworld yaml");
    }

    @Test
    void whenCreateFlowWithTemplate_thenFlowCreatedWithTemplateYamlContent() {
        var id = createFlow.invoke(new CreateFlowParam("test flow", "helloworld", null));

        var f = fetchFlow.invoke(id);
        assertNotNull(f);

        assertEquals("helloworld yaml", f.getYaml());
        assertNotNull(f.getCreatedBy());
        assertNotNull(f.getUpdatedBy());
    }

    @Test
    void whenCreateFlowWithDuplicatedName_thenThrowException() {
        createFlow.invoke(new CreateFlowParam("flow A", null, null));

        assertThrows(DuplicateException.class, () ->
                createFlow.invoke(new CreateFlowParam("flow A", null, null)));
    }
}
