package com.flowci.build.business;

import com.flowci.build.model.Build;

public interface FetchBuild {
    Build invoke(Long buildId);
}
