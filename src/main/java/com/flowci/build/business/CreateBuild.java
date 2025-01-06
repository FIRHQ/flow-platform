package com.flowci.build.business;

import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;

public interface CreateBuild {
    Build invoke(Long flowId, Build.Trigger trigger, Variables inputs);
}
