package com.flowci.build.business;

import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;

public interface CreateNewBuild {
    /**
     * Trigger a new build
     *
     * @param flowId  flow id
     * @param trigger trigger
     * @param inputs  extra variables
     * @return build object
     */
    Build invoke(Long flowId, Build.Trigger trigger, Variables inputs);
}
