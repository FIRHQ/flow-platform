package com.flowci;

import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.BuildYamlRepo;
import com.flowci.flow.repo.FlowRepo;
import com.flowci.flow.repo.FlowUserRepo;
import com.flowci.flow.repo.FlowYamlRepo;
import lombok.Getter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@Import(SpringTest.MockRepositoriesConfig.class)
public abstract class SpringTest {

    @Getter
    @TestConfiguration
    public static class MockRepositoriesConfig {

        @MockBean
        private FlowRepo flowRepo;

        @MockBean
        private FlowYamlRepo flowYamlRepo;

        @MockBean
        private FlowUserRepo flowUserRepo;

        @MockBean
        private BuildRepo buildRepo;

        @MockBean
        private BuildYamlRepo buildYamlRepo;
    }

    @DynamicPropertySource
    static void initTestProperties(DynamicPropertyRegistry registry) {
        registry.add("flowci.init.root-group", () -> "false");
    }

    protected static InputStream getResource(String path) {
        return SpringTest.class.getClassLoader().getResourceAsStream(path);
    }

    protected static String getResourceAsString(String path) {
        try {
            return StreamUtils.copyToString(getResource(path), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}