package com.flowci.core.job.domain;

import com.flowci.sm.Context;
import com.flowci.zookeeper.InterLock;

public class JobSmContext extends Context {

    public Job job;

    public String yml;

    public Step step;

    public Job.Status getTargetToJobStatus() {
        String name = this.to.getName();
        return Job.Status.valueOf(name);
    }
}
