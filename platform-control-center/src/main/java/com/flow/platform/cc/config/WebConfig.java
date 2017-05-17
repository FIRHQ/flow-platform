package com.flow.platform.cc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@Configuration
@EnableWebMvc
@ComponentScan({
        "com.flow.platform.cc",
        "com.flow.platform.cc.service"})
public class WebConfig extends WebMvcConfigurerAdapter {

    private static final String SPRING_ENV = "spring.profiles.active";
    private static final String CC_ENV = "flow.cc.env";
    private static final String DEFAULT_CC_ENV = "local";

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        String env = System.getProperty(SPRING_ENV);
        if (env == null) {
            env = System.getProperty(CC_ENV, DEFAULT_CC_ENV);
        }

        String envPropertiesFile = String.format("app-%s.properties", env);
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource(envPropertiesFile));
        return configurer;
    }
}
