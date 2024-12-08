package com.flowci.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String YAML_TEMPLATE_CACHE_MANAGER = "yamlTemplateCacheManager";

    @Bean
    public CacheManager yamlTemplateCacheManager(AppProperties appProperties) {
        var expireInMinutes = appProperties.getTemplates().getCacheExpireInMinutes();

        var caffeine = Caffeine.newBuilder()
                .expireAfterWrite(expireInMinutes, TimeUnit.MINUTES)
                .initialCapacity(20);

        var cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
