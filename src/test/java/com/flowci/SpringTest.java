package com.flowci;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@SpringBootTest
public abstract class SpringTest {

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
