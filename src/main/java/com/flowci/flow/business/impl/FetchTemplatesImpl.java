package com.flowci.flow.business.impl;

import com.flowci.common.config.AppProperties;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.business.FetchTemplates;
import com.flowci.flow.model.YamlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.flowci.common.config.CacheConfig.YAML_TEMPLATE_CACHE_MANAGER;

@Slf4j
@Service
public class FetchTemplatesImpl implements FetchTemplates {

    private final RestClient restClient = RestClient.create();

    private final ParameterizedTypeReference<List<YamlTemplate>> responseType =
            new ParameterizedTypeReference<>() {
            };

    private final AppProperties appProperties;

    public FetchTemplatesImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    @Cacheable(cacheManager = YAML_TEMPLATE_CACHE_MANAGER, cacheNames = "templates.json")
    public List<YamlTemplate> invoke() {
        var url = appProperties.getTemplates().getUrl();

        return restClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    var msg = "Unable to fetch yaml templates from " + url;
                    log.error(msg);
                    throw new NotAvailableException(msg);
                })
                .body(responseType);
    }
}
