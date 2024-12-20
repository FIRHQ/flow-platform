package com.flowci.yaml.business;

import com.flowci.yaml.model.FlowV2;

public interface ParseYamlV2 {
    FlowV2 invoke(String yaml);
}
