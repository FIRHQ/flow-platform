package com.flowci.flow.business;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactIgnore;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.flowci.SpringTest;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.model.YamlTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@PactConsumerTest
class FetchTemplateContentTest extends SpringTest {

    @MockBean
    private FetchTemplates fetchTemplates;

    @Autowired
    private FetchTemplateContent fetchTemplateContent;

    @Autowired
    private CacheManager yamlTemplateCacheManager;

    @Pact(provider = "TemplateContentProvider", consumer = "consumer_template_content")
    V4Pact createPact(PactDslWithProvider builder) throws IOException {
        return builder
                .given("maven template")
                .uponReceiving("")
                .path("/git/templates/helloworld.yaml")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/yaml"))
                .body(getResourceAsString("template_helloworld.yaml"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(providerName = "TemplateContentProvider")
    void whenFetchingContentSuccessfully_thenReturnYamlContentFromPact(MockServer mockServer) {
        var mockUrl = mockServer.getUrl() + "/git/templates/helloworld.yaml";
        when(fetchTemplates.invoke())
                .thenReturn(List.of(
                        new YamlTemplate(
                                "helloworld",
                                "",
                                mockUrl,
                                false
                        )
                ));

        var content = fetchTemplateContent.invoke("helloworld");
        assertEquals(getResourceAsString("template_helloworld.yaml"), content);

        var cache = yamlTemplateCacheManager.getCache("template.content");
        var cached = cache.get("helloworld");
        assertNotNull(cached);
    }

    @Test
    @PactIgnore
    void whenFetchingContentNotFound_thenThrowException() {
        when(fetchTemplates.invoke()).thenReturn(Collections.emptyList());
        assertThrows(NotAvailableException.class, () -> fetchTemplateContent.invoke("helloworld"));
    }
}
