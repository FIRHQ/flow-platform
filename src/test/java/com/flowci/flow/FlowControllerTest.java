package com.flowci.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.SpringTest;
import com.flowci.common.model.ErrorResponse;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchFlow;
import com.flowci.flow.business.FetchFlowYamlContent;
import com.flowci.flow.business.UpdateFlowYamlContent;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.Base64;

import static com.flowci.TestUtils.newDummyInstance;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlowControllerTest extends SpringTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateFlow createFlow;

    @MockBean
    private FetchFlow fetchFlow;

    @MockBean
    private FetchFlowYamlContent fetchFlowYamlContent;

    @MockBean
    private UpdateFlowYamlContent updateFlowYamlContent;

    @Test
    void givenCreateFlowParameter_whenCreateFlow_thenReturnFlowId() throws Exception {
        var expectedFlow = new Flow();
        expectedFlow.setName("hello_world");
        expectedFlow.setId(100L);

        when(createFlow.invoke(any())).thenReturn(expectedFlow);

        var param = new CreateFlowParam(expectedFlow.getName(), null, null);
        var r = mvc.perform(post("/v2/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(param)))
                .andExpect(status().isOk())
                .andReturn();

        var fetched = objectMapper.readValue(r.getResponse().getContentAsString(), Flow.class);
        assertEquals(expectedFlow.getId(), fetched.getId());
        assertEquals(expectedFlow.getName(), fetched.getName());
    }

    @Test
    void givenCreateFlowParameterWithInvalidName_whenCreateFlow_thenReturnError() throws Exception {
        var param = new CreateFlowParam("hello!!!!", null, null);
        var r = mvc.perform(post("/v2/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(param)))
                .andExpect(status().isBadRequest())
                .andReturn();

        var content = r.getResponse().getContentAsByteArray();
        var error = objectMapper.readValue(content, ErrorResponse.class);
        assertEquals("invalid flow name", error.message());
        assertEquals(400, error.code());
    }

    @Test
    void givenCreateFlowParameterWithDuplicatedName_whenCreateFlow_thenReturnError() throws Exception {
        var sqlEx = mock(SQLException.class);
        when(sqlEx.getSQLState()).thenReturn("23505");
        when(createFlow.invoke(any()))
                .thenThrow(new DataIntegrityViolationException("data integrity violation exception", sqlEx));

        var param = new CreateFlowParam("helloworld", null, null);
        var r = mvc.perform(post("/v2/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(param)))
                .andExpect(status().isBadRequest())
                .andReturn();

        var content = r.getResponse().getContentAsByteArray();
        var error = objectMapper.readValue(content, ErrorResponse.class);
        assertEquals("duplicate flow name", error.message());
        assertEquals(400, error.code());
    }

    @Test
    void givenCreateFlowParameter_whenCreateFlowWithUnexpectedError_thenReturnError() throws Exception {
        when(createFlow.invoke(any()))
                .thenThrow(new RuntimeException("something went wrong"));

        var param = new CreateFlowParam("helloworld", null, null);
        var r = mvc.perform(post("/v2/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(param)))
                .andExpect(status().is5xxServerError())
                .andReturn();

        var content = r.getResponse().getContentAsByteArray();
        var error = objectMapper.readValue(content, ErrorResponse.class);
        assertEquals("something went wrong", error.message());
        assertEquals(500, error.code());
    }

    @Test
    void givenFlowName_whenFetching_thenReturnFlow() throws Exception {
        var mockFlow = newDummyInstance(Flow.class)
                .set(field(Flow::getName), "hello_world")
                .create();

        when(fetchFlow.invoke(anyString())).thenReturn(mockFlow);

        var r = mvc.perform(get(String.format("/v2/flows/%s", mockFlow.getName())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var fetched = objectMapper.readValue(r.getResponse().getContentAsString(), Flow.class);
        assertEquals(mockFlow.getId(), fetched.getId());
    }

    @Test
    void givenFlowName_whenFetchingYaml_thenReturnBase64EncodedYaml() throws Exception {
        var mockFlow = newDummyInstance(Flow.class)
                .set(field(Flow::getName), "some_name")
                .create();

        var expectedYaml = Base64.getEncoder().encodeToString("some yaml".getBytes());
        when(fetchFlow.invoke(anyString())).thenReturn(mockFlow);
        when(fetchFlowYamlContent.invoke(any(), eq(true))).thenReturn(expectedYaml);

        var r = mvc.perform(get(String.format("/v2/flows/%s/yaml", mockFlow.getName())))
                .andExpectAll(
                        status().is2xxSuccessful(),
                        header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn();

        assertEquals(expectedYaml, r.getResponse().getContentAsString());
    }

    @Test
    void givenFlowNameAndYaml_whenUpdating_thenYamlIsUpdated() throws Exception {
        var mockFlow = newDummyInstance(Flow.class)
                .set(field(Flow::getName), "some_name")
                .create();
        when(fetchFlow.invoke(anyString())).thenReturn(mockFlow);

        var expectedYaml = Base64.getEncoder().encodeToString("some yaml".getBytes());

        var yamlCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(updateFlowYamlContent).invoke(any(), yamlCaptor.capture());

        mvc.perform(post(String.format("/v2/flows/%s/yaml", mockFlow.getName()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(expectedYaml))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedYaml, yamlCaptor.getValue());
    }
}
