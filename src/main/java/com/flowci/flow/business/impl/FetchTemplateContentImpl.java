package com.flowci.flow.business.impl;

import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.business.FetchTemplateContent;
import com.flowci.flow.business.FetchTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.flowci.common.config.CacheConfig.YAML_TEMPLATE_CACHE_MANAGER;

@Slf4j
@Component
public class FetchTemplateContentImpl implements FetchTemplateContent {

    private final RestClient restClient = RestClient.create();

    private final FetchTemplates fetchTemplates;

    public FetchTemplateContentImpl(FetchTemplates fetchTemplates) {
        this.fetchTemplates = fetchTemplates;
    }

    @Override
    @Cacheable(cacheManager = YAML_TEMPLATE_CACHE_MANAGER, cacheNames = "template.content", key = "#p0")
    public String invoke(String template) {
        for (var t : fetchTemplates.invoke()) {
            if (t.title().equals(template)) {
                return restClient.get()
                        .uri(t.sourceUrl())
                        .retrieve()
                        .body(String.class);
            }
        }

        var error = String.format("Template '%s' not found", template);
        log.warn(error);
        throw new NotAvailableException(error);
    }
}
