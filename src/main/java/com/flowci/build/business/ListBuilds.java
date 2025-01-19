package com.flowci.build.business;

import com.flowci.build.model.Build;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface ListBuilds {
    List<Build> invoke(PageRequest pageRequest);
    List<Build> invoke(Build.Status status, PageRequest pageRequest);
}
