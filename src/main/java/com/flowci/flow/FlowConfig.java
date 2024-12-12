package com.flowci.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class FlowConfig {

    @Bean
    public RestClient templateRestClient(ObjectMapper objectMapper) {
        var converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN
        ));

        return RestClient.builder()
                .messageConverters(httpMessageConverters -> {
                    httpMessageConverters.clear();
                    httpMessageConverters.add(converter);
                }).build();
    }
}
