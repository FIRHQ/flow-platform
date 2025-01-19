package com.flowci.build.business.impl;

import com.flowci.build.business.ListBuilds;
import com.flowci.build.model.Build;
import com.flowci.build.repo.BuildRepo;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class ListBuildsImpl implements ListBuilds {

    private final BuildRepo buildRepo;

    @Override
    public List<Build> invoke(PageRequest pageRequest) {
        return buildRepo.findAll(pageRequest).getContent();
    }

    @Override
    public List<Build> invoke(Build.Status status, PageRequest pageRequest) {
        return buildRepo.findAllByStatusOrderByCreatedAtDesc(status, pageRequest);
    }
}
