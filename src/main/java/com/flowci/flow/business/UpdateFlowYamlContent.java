package com.flowci.flow.business;

public interface UpdateFlowYamlContent {
    /**
     * Update flow YAML
     * @param id flow id
     * @param b64Yaml YAML with Base64 format
     */
    void invoke(Long id, String b64Yaml);
}
