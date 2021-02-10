/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.service.LocalTaskService;
import com.flowci.core.job.service.ReportService;
import com.flowci.core.user.domain.User;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/jobs")
public class JobController extends BaseController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private LocalTaskService localTaskService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @GetMapping("/{flow}")
    @Action(JobAction.LIST)
    public Page<JobItem> list(@PathVariable("flow") String name,
                              @RequestParam(required = false, defaultValue = DefaultPage) int page,
                              @RequestParam(required = false, defaultValue = DefaultSize) int size) {

        Flow flow = flowService.get(name);
        return jobService.list(flow, page, size);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}")
    @Action(JobAction.GET)
    public Job get(@PathVariable("flow") String name, @PathVariable String buildNumberOrLatest) {
        return super.getJob(name, buildNumberOrLatest);
    }

    @GetMapping("/{jobId}/desc")
    @Action(JobAction.GET)
    public JobDesc getDesc(@PathVariable String jobId) {
        return jobService.getDesc(jobId);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(JobAction.GET_YML)
    public String getYml(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        JobYml yml = jobService.getYml(job);
        return Base64.getEncoder().encodeToString(yml.getRaw().getBytes());
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/steps")
    @Action(JobAction.LIST_STEPS)
    public List<Step> listSteps(@PathVariable String flow,
                                @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return stepService.list(job);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/tasks")
    @Action(JobAction.LIST_STEPS)
    public List<ExecutedLocalTask> listTasks(@PathVariable String flow,
                                             @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return localTaskService.list(job);
    }

    @PostMapping
    @Action(JobAction.CREATE)
    public Job create(@Validated @RequestBody CreateJob data) {
        Flow flow = flowService.get(data.getFlow());
        String b64Yaml = ymlService.getYmlString(flow.getId(), Yml.DEFAULT_NAME);
        return jobService.create(flow, StringHelper.fromBase64(b64Yaml), Trigger.API, data.getInputs());
    }

    @PostMapping("/run")
    @Action(JobAction.RUN)
    public void createAndStart(@Validated @RequestBody CreateJob body) {
        User current = sessionManager.get();
        Flow flow = flowService.get(body.getFlow());
        String b64Yml = ymlService.getYmlString(flow.getId(), Yml.DEFAULT_NAME);
        if (!StringHelper.hasValue(b64Yml)) {
            throw new NotFoundException("YAML not found");
        }

        String ymlStr = StringHelper.fromBase64(b64Yml);

        // start from thread since could be loading yaml from git repo
        appTaskExecutor.execute(() -> {
            sessionManager.set(current);
            Job job = jobService.create(flow, ymlStr, Trigger.API, body.getInputs());
            jobService.start(job);
        });
    }

    @PostMapping("/rerun")
    @Action(JobAction.RUN)
    public void rerun(@Validated @RequestBody RerunJob body) {
        Job job = jobService.get(body.getJobId());
        Flow flow = flowService.getById(job.getFlowId());
        jobService.rerun(flow, job);
    }

    @PostMapping("/{flow}/{buildNumber}/cancel")
    @Action(JobAction.CANCEL)
    public Job cancel(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        jobService.cancel(job);
        return job;
    }

    @GetMapping("/{flow}/{buildNumber}/reports")
    @Action(JobAction.LIST_REPORTS)
    public List<JobReport> listReports(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        return reportService.list(job);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/reports/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(JobAction.FETCH_REPORT)
    public String fetchReport(@PathVariable String flow,
                              @PathVariable String buildNumber,
                              @PathVariable String reportId) {
        Job job = get(flow, buildNumber);
        return reportService.fetch(job, reportId);
    }
}
