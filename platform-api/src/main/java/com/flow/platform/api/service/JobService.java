/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flow.platform.api.service;

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.domain.CmdBase;
import java.util.List;

/**
 * @author yh@firim
 */
public interface JobService {

    /**
     * create job
     *
     * @param nodePath node path
     * @return job
     */
    Job createJob(String nodePath);

    /**
     * handle callback
     *
     * @param identifier if node callback identifier is nodePath, if session callback identifier is jobId
     */
    void callback(String identifier, CmdBase cmdBase);

    /**
     * run node
     */
    void run(JobNode node);


    /**
     * deep copy node
     *
     * @param nodePath can copy every node
     */
    JobFlow createJobNode(String nodePath);

    /**
     * save job
     */
    Job save(Job job);

    /**
     * find job by id
     */
    Job find(String id);

    /**
     * update job
     */
    Job update(Job job);

    /**
     * show job's job step
     */
    List<JobStep> listJobStep(String jobId);
}
