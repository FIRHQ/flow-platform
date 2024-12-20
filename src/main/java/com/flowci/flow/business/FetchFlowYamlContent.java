package com.flowci.flow.business;

public interface FetchFlowYamlContent {
    /**
     * Fetch yaml content
     * @param id flow id
     * @return YAML with Base64 format
     */
    String invoke(Long id);
}
