package com.flowci.flow.business;

public interface FetchFlowYamlContent {
    /**
     * Fetch yaml content
     * @param id flow id
     * @param returnB64 whether return base64 content
     * @return YAML or with Base64 format
     */
    String invoke(Long id, boolean returnB64);
}
