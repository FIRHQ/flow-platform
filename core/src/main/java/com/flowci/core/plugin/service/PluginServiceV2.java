package com.flowci.core.plugin.service;

import org.springframework.core.io.Resource;

public interface PluginServiceV2 {

    void load(Resource repoUri);
}
