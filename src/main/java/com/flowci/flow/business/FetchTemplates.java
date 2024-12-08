package com.flowci.flow.business;

import com.flowci.flow.model.YamlTemplate;

import java.util.List;

/**
 * Fetch template configuration
 */
public interface FetchTemplates {
    List<YamlTemplate> invoke();
}
