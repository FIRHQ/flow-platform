package com.flowci;

import com.flowci.flow.repo.FlowRepo;
import com.flowci.flow.repo.FlowYamlRepo;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public abstract class SpringTest {

    @MockBean
    private FlowRepo flowRepo;

    @MockBean
    private FlowYamlRepo flowYamlRepo;

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
