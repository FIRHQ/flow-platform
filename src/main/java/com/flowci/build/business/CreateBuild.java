package com.flowci.build.business;

import com.flowci.build.model.Build;
import com.flowci.common.model.Variables;
import jakarta.annotation.Nullable;

public interface CreateBuild {
    Build invoke(Long flowId, Build.Trigger trigger, @Nullable Variables inputs);
}
