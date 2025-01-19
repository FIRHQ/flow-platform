package com.flowci.build.business.impl;

import com.flowci.build.business.FetchBuild;
import com.flowci.build.model.Build;
import com.flowci.build.repo.BuildRepo;
import com.flowci.common.exception.NotAvailableException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FetchBuildImpl implements FetchBuild {

    private final BuildRepo buildRepo;

    @Override
    public Build invoke(Long buildId) {
        var optional = buildRepo.findById(buildId);
        if (optional.isEmpty()) {
            throw new NotAvailableException("Build not found");
        }
        return optional.get();
    }
}
