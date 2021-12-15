package com.flowci.core.job.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "job")
public class JobDesc {

    @Id
    protected String id;

    protected Long buildNumber;

    protected String flowId;

    protected String flowName;

    public static JobDesc create(Job job) {
        var desc = new JobDesc();
        desc.setId(job.getId());
        desc.setBuildNumber(job.getBuildNumber());
        desc.setFlowId(job.getFlowId());
        desc.setFlowName(job.getFlowName());
        return desc;
    }
}
