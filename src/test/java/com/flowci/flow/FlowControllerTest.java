package com.flowci.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.model.ErrorResponse;
import com.flowci.flow.business.CreateFlow;
import com.flowci.flow.business.FetchTemplates;
import com.flowci.flow.model.CreateFlowParam;
import com.flowci.flow.model.Flow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FlowController.class)
class FlowControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateFlow createFlow;

    // keep it !!
    @MockBean
    private FetchTemplates fetchTemplates;

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
}
