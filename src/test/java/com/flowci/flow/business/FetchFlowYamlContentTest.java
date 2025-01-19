package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.TestUtils;
import com.flowci.flow.model.FlowYaml;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;
import java.util.Optional;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FetchFlowYamlContentTest extends SpringTest {

    @Autowired
    private MockRepositoriesConfig repositoriesConfig;

    @Autowired
    private FetchFlowYamlContent fetchFlowYamlContent;

    @Test
    void whenFetching_thenReturnYamlEncodedWithBase64() {
        var expectedYaml = "some yaml";
        var mockFlowYaml = TestUtils.newDummyInstance(FlowYaml.class)
                .set(field(FlowYaml::getYaml), expectedYaml)
                .create();

        var mockFlowYamlRepo = repositoriesConfig.getFlowYamlRepo();
        when(mockFlowYamlRepo.findById(any())).thenReturn(Optional.of(mockFlowYaml));

        var base64Yaml = fetchFlowYamlContent.invoke(1L, true);
        assertEquals(Base64.getEncoder().encodeToString(expectedYaml.getBytes()), base64Yaml);
    }
}
