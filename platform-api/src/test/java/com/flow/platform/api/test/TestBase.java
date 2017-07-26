package com.flow.platform.api.test;/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.flow.platform.api.config.WebConfig;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


/**
 * @author yh@fir.im
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebConfig.class}, initializers = {TestBase.MockContextInitializer.class})
@PropertySource("classpath:app-default.properties")
public abstract class TestBase {

    static {
        System.setProperty("flow.api.env", "test");
        System.setProperty("flow.api.task.keep_idle_agent", "false");
    }

    public static class MockContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {

        }
    }

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    protected Gson gsonConfig;

    protected MockMvc mockMvc;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @After
    public void afterEach() {
    }

    @AfterClass
    public static void afterClass() throws IOException {
        // clean up cmd log folder
    }
}