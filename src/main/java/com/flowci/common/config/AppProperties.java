package com.flowci.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("flowci")
public class AppProperties {

    private Templates templates;

    @Data
    public static class Templates {

        private String url;

        private int cacheExpireInMinutes = 60;
    }
}
