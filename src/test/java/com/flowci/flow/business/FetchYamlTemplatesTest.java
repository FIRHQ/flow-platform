package com.flowci.flow.business;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.flowci.SpringTest;
import com.flowci.common.config.AppProperties;
import com.flowci.common.exception.NotAvailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@PactConsumerTest
class FetchYamlTemplatesTest extends SpringTest {

    @MockBean
    private AppProperties appProperties;

    @Autowired
    private FetchYamlTemplates fetchYamlTemplates;

    @Pact(provider = "YamlTemplateProvider", consumer = "consumer_yaml_template")
    V4Pact createPact(PactDslWithProvider builder) throws IOException {
        return builder
                .given("yaml templates are fetched")
                .uponReceiving("")
                .path("/git/templates.json")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
                .body(getResourceAsString("flow_templates.json"))
                .toPact(V4Pact.class);
    }

    @Pact(provider = "YamlTemplateProviderWith5xx", consumer = "consumer_yaml_template")
    V4Pact createPactWith5xx(PactDslWithProvider builder) throws IOException {
        return builder
                .given("yaml templates are fetched")
                .uponReceiving("")
                .path("/git/templates.json")
                .method("GET")
                .willRespondWith()
                .status(500)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(providerName = "YamlTemplateProvider")
    void whenFetchingSuccessfully_thenReturnListOfTemplates(MockServer mockServer) {
        var mockTemplatesConfig = new AppProperties.Templates();
        mockTemplatesConfig.setUrl(mockServer.getUrl() + "/git/templates.json");

        when(appProperties.getTemplates()).thenReturn(mockTemplatesConfig);

        var templates = fetchYamlTemplates.invoke();
        assertNotNull(templates);
        assertEquals(2, templates.size());

        // call second time, should be load from cache
        fetchYamlTemplates.invoke();
        verify(appProperties, times(1)).getTemplates();
    }

    @Test
    @PactTestFor(providerName = "YamlTemplateProviderWith5xx")
    void whenFetchingWith5xx_thenReturnListOfTemplates(MockServer mockServer) {
        var mockTemplatesConfig = new AppProperties.Templates();
        mockTemplatesConfig.setUrl(mockServer.getUrl() + "/git/templates.json");

        when(appProperties.getTemplates()).thenReturn(mockTemplatesConfig);

        assertThrows(NotAvailableException.class, () -> fetchYamlTemplates.invoke());
    }
}
