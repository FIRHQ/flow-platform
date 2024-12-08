package com.flowci.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.ObjectMapperFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.instance();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();

                converters.addAll(List.of(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(ObjectMapperFactory.instance()),
                        new ResourceHttpMessageConverter(),
                        new AllEncompassingFormHttpMessageConverter(),
                        new StringHttpMessageConverter()
                ));
            }
        };
    }
}
