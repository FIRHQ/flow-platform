package com.flowci.build.business;

import com.flowci.build.model.Build;

public interface FetchYamlFromGit {
    void invoke(Build build);
}
