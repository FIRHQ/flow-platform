package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.common.RequestContextHolder;
import com.flowci.flow.model.FlowYaml;
import com.flowci.yaml.business.ParseYamlV2;
import com.flowci.yaml.model.FlowV2;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Base64;
import java.util.Optional;

import static com.flowci.TestUtils.newDummyInstance;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateFlowYamlContentTest extends SpringTest {

    @Autowired
    private UpdateFlowYamlContent updateFlowYamlContent;

    @Autowired
    private MockRepositoriesConfig mockRepositoriesConfig;

    @MockBean
    private ParseYamlV2 mockParseYamlV2;

    @MockBean
    private RequestContextHolder mockRequestContextHolder;

    @Test
    void givenYaml_whenUpdating_thenUpdated() {
        var mockUserId = 10L;
        var mockFlowId = 20L;

        var mockFlowYaml = newDummyInstance(FlowYaml.class)
                .set(field(FlowYaml::getId), mockFlowId)
                .set(field(FlowYaml::getYaml), "yaml")
                .set(field(FlowYaml::getUpdatedBy), 1L)
                .create();

        when(mockRequestContextHolder.getUserId()).thenReturn(mockUserId);
        when(mockParseYamlV2.invoke(any())).thenReturn(mock(FlowV2.class));

        var flowYamlCaptor = ArgumentCaptor.forClass(FlowYaml.class);
        var mockFlowYamlRepo = mockRepositoriesConfig.getFlowYamlRepo();
        when(mockFlowYamlRepo.findById(eq(mockFlowId))).thenReturn(Optional.of(mockFlowYaml));
        when(mockFlowYamlRepo.save(flowYamlCaptor.capture())).thenReturn(mockFlowYaml);

        var base64Yaml = Base64.getEncoder().encodeToString("updated yaml".getBytes());
        assertDoesNotThrow(() -> updateFlowYamlContent.invoke(mockFlowId, base64Yaml));
        assertEquals("updated yaml", flowYamlCaptor.getValue().getYaml());
        assertEquals(mockUserId, flowYamlCaptor.getValue().getUpdatedBy());
    }
}
