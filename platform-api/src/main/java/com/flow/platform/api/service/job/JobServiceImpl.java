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
package com.flow.platform.api.service.job;

import static com.flow.platform.api.domain.envs.FlowEnvs.FLOW_STATUS;
import static com.flow.platform.api.domain.envs.FlowEnvs.FLOW_YML_STATUS;
import static com.flow.platform.api.domain.envs.FlowEnvs.StatusValue;
import static com.flow.platform.api.domain.job.NodeStatus.FAILURE;
import static com.flow.platform.api.domain.job.NodeStatus.STOPPED;
import static com.flow.platform.api.domain.job.NodeStatus.SUCCESS;
import static com.flow.platform.api.domain.job.NodeStatus.TIMEOUT;

import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.dao.job.JobYmlDao;
import com.flow.platform.api.dao.job.NodeResultDao;
import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.JobEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.events.JobStatusChangeEvent;
import com.flow.platform.api.git.GitEventEnvConverter;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitEventType;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class JobServiceImpl extends ApplicationEventService implements JobService {

    private static Logger LOGGER = new Logger(JobService.class);

    private Integer RETRY_TIMEs = 5;

    private final Integer createSessionRetryTimes = 5;

    @Value("${task.job.toggle.execution_timeout}")
    private Boolean isJobTimeoutExecuteTimeout;

    @Value("${task.job.toggle.execution_create_session_duration}")
    private Long jobExecuteTimeoutCreateSessionDuration;

    @Value("${task.job.toggle.execution_running_duration}")
    private Long jobExecuteTimeoutRunningDuration;

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private PlatformQueue<CmdCallbackQueueItem> cmdCallbackQueue;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private GitService gitService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private JobYmlDao jobYmlDao;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public Job find(String flowName, Integer number) {
        Job job = jobDao.get(flowName, number);
        return find(job);
    }

    @Override
    public Job find(BigInteger jobId) {
        Job job = jobDao.get(jobId);
        return find(job);
    }

    @Override
    public String findYml(String path, Integer number) {
        Job job = find(path, number);
        return jobNodeService.find(job).getFile();
    }

    @Override
    public List<NodeResult> listNodeResult(String path, Integer number) {
        Job job = find(path, number);
        return nodeResultService.list(job, true);
    }

    @Override
    public List<Job> list(List<String> paths, boolean latestOnly) {
        if (latestOnly) {
            return jobDao.latestByPath(paths);
        }
        return jobDao.listByPath(paths);
    }

    @Override
    @Transactional(noRollbackFor = FlowException.class)
    public Job createJob(String path, GitEventType eventType, Map<String, String> envs, User creator) {
        Node root = nodeService.find(PathUtil.rootPath(path));
        if (root == null) {
            throw new IllegalParameterException("Path does not existed");
        }

        if (creator == null) {
            throw new IllegalParameterException("User is required while create job");
        }

        String status = root.getEnv(FLOW_STATUS);
        if (Strings.isNullOrEmpty(status) || !status.equals(StatusValue.READY.value())) {
            throw new IllegalStatusException("Cannot create job since status is not READY");
        }

        // create job
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath(root.getPath());
        job.setNodeName(root.getName());
        job.setNumber(jobDao.maxBuildNumber(job.getNodePath()) + 1);
        job.setCategory(eventType);
        job.setCreatedBy(creator.getEmail());
        job.setCreatedAt(ZonedDateTime.now());
        job.setUpdatedAt(ZonedDateTime.now());

        // setup job env variables
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_CATEGORY, eventType.name());
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_NUMBER, job.getNumber().toString());
        job.putEnv(JobEnvs.FLOW_JOB_LOG_PATH, logUrl(job));

        EnvUtil.merge(root.getEnvs(), job.getEnvs(), true);
        EnvUtil.merge(envs, job.getEnvs(), true);

        //save job
        return jobDao.save(job);
    }

    @Override
    @Transactional(noRollbackFor = FlowException.class)
    public void createJobAndYmlLoad(String path,
                                    GitEventType eventType,
                                    Map<String, String> envs,
                                    User creator,
                                    Consumer<Job> onJobCreated) {

        // find flow and reset yml status
        Flow flow = nodeService.findFlow(path);
        nodeService.addFlowEnv(flow, EnvUtil.build(FLOW_YML_STATUS, YmlStatusValue.NOT_FOUND));

        // merge input env to flow for git loading, not save to flow since the envs is for job
        EnvUtil.merge(envs, flow.getEnvs(), true);

        //create job
        Job job = createJob(path, eventType, envs, creator);
        updateJobStatusAndSave(job, JobStatus.YML_LOADING);

        // load yml
        ymlService.loadYmlContent(flow, yml -> {
            LOGGER.trace("Yml content has been loaded for path : " + path);
            Node root = nodeService.find(PathUtil.rootPath(path));

            // set git commit info to job env
            if (eventType == GitEventType.MANUAL) {
                GitCommit gitCommit = gitService.latestCommit(flow);
                Map<String, String> envFromCommit = GitEventEnvConverter.convert(gitCommit);
                EnvUtil.merge(envFromCommit, job.getEnvs(), true);
                jobDao.update(job);
            }

            String loadedYml = null;
            try {
                loadedYml = ymlService.getYmlContent(root);
                if (Strings.isNullOrEmpty(loadedYml)) {
                    throw new IllegalStatusException("Yml is loading for path " + path);
                }
            } catch (FlowException e) {
                job.setFailureMessage(e.getMessage());
                updateJobStatusAndSave(job, JobStatus.FAILURE);
            }

            //create yml snapshot for job
            jobNodeService.save(job, loadedYml);

            // init for node result and set to job object
            List<NodeResult> resultList = nodeResultService.create(job);
            NodeResult rootResult = resultList.remove(resultList.size() - 1);
            job.setRootResult(rootResult);
            job.setChildrenResult(resultList);

            // to create agent session for job
            try {
                String sessionId = cmdService.createSession(job, createSessionRetryTimes);
                job.setSessionId(sessionId);
                updateJobStatusAndSave(job, JobStatus.SESSION_CREATING);
            } catch (IllegalStatusException e) {
                job.setFailureMessage(e.getMessage());
                updateJobStatusAndSave(job, JobStatus.FAILURE);
            }

            try {
                if (onJobCreated != null) {
                    onJobCreated.accept(job);
                }
            } catch (Throwable e) {
                LOGGER.warn("Fail to create job for path %s : %s ", path, ExceptionUtil.findRootCause(e).getMessage());
            }

        });
    }

    @Override
    public void callback(CmdCallbackQueueItem cmdQueueItem) {
        BigInteger jobId = cmdQueueItem.getJobId();
        Cmd cmd = cmdQueueItem.getCmd();
        Job job = jobDao.get(jobId);

        if (Job.FINISH_STATUS.contains(job.getStatus())) {
            LOGGER.trace("Reject cmd callback since job %s already in finish status", job.getId());
            return;
        }

        if (cmd.getType() == CmdType.CREATE_SESSION) {
            onCreateSessionCallback(job, cmd);
            return;
        }

        if (cmd.getType() == CmdType.RUN_SHELL) {
            String path = cmd.getExtra();
            if (Strings.isNullOrEmpty(path)) {
                throw new IllegalParameterException("Node path is required for cmd RUN_SHELL callback");
            }

            onRunShellCallback(path, cmd, job);
            return;
        }

        if (cmd.getType() == CmdType.DELETE_SESSION) {
            LOGGER.trace("Session been deleted for job: %s", cmdQueueItem.getJobId());
            return;
        }

        LOGGER.warn("not found cmdType, cmdType: %s", cmd.getType().toString());
        throw new NotFoundException("not found cmdType");
    }

    @Override
    public void deleteJob(String path) {
        List<BigInteger> jobIds = jobDao.findJobIdsByPath(path);
        // TODO :  Late optimization and paging jobIds
        if (jobIds.size() > 0) {
            jobYmlDao.delete(jobIds);
            nodeResultDao.delete(jobIds);
            jobDao.deleteJob(path);
        }
    }

    /**
     * run node
     *
     * @param node job node's script and record cmdId and sync send http
     */
    private void run(Node node, Job job) {
        if (node == null) {
            throw new IllegalParameterException("Cannot run node with null value");
        }

        NodeTree tree = jobNodeService.get(job);

        if (!tree.canRun(node.getPath())) {
            // run next node
            Node next = tree.next(node.getPath());
            run(next, job);
            return;
        }

        // pass job env to node
        EnvUtil.merge(job.getEnvs(), node.getEnvs(), false);

        // to run node with customized cmd id
        try {
            NodeResult nodeResult = nodeResultService.find(node.getPath(), job.getId());
            CmdInfo cmd = cmdService.runShell(job, node, nodeResult.getCmdId());
        } catch (IllegalStatusException e) {
            CmdInfo rawCmd = (CmdInfo) e.getData();
            rawCmd.setStatus(CmdStatus.EXCEPTION);
            nodeResultService.updateStatusByCmd(job, node, Cmd.convert(rawCmd), e.getMessage());
        }
    }

    /**
     * Create session callback
     */
    private void onCreateSessionCallback(Job job, Cmd cmd) {
        if (cmd.getStatus() != CmdStatus.SENT) {

            if (cmd.getRetry() > 1) {
                LOGGER.trace("Create session failure but retrying: %s", cmd.getStatus().getName());
                return;
            }

            final String errMsg = "Create session failure with cmd status: " + cmd.getStatus().getName();
            LOGGER.warn(errMsg);

            job.setFailureMessage(errMsg);
            updateJobStatusAndSave(job, JobStatus.FAILURE);
            return;
        }

        // run step
        NodeTree tree = jobNodeService.get(job);
        if (tree == null) {
            throw new NotFoundException("Cannot fond related node tree for job: " + job.getId());
        }

        // set job properties
        job.setSessionId(cmd.getSessionId());
        job.putEnv(JobEnvs.FLOW_JOB_AGENT_INFO, cmd.getAgentPath().toString());
        updateJobStatusAndSave(job, JobStatus.RUNNING);

        // start run flow from fist node
        run(tree.first(), job);
    }

    /**
     * Run shell callback
     */
    private void onRunShellCallback(String path, Cmd cmd, Job job) {
        NodeTree tree = jobNodeService.get(job);
        Node node = tree.find(path);
        Node next = tree.next(path);

        // bottom up recursive update node result
        NodeResult nodeResult = nodeResultService.updateStatusByCmd(job, node, cmd, null);
        LOGGER.debug("Run shell callback for node result: %s", nodeResult);

        // no more node to run and status is not running
        if (next == null && !nodeResult.isRunning()) {
            stopJob(job);
            return;
        }

        // continue to run if on success status
        if (nodeResult.isSuccess()) {
            run(next, job);
            return;
        }

        // continue to run if allow failure on failure status
        if (nodeResult.isFailure()) {
            if (node instanceof Step) {
                Step step = (Step) node;
                if (step.getAllowFailure()) {
                    run(next, job);
                }

                // clean up session if node result failure and set job status to error

                //TODO: Missing unit test
                else {
                    stopJob(job);
                }
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void enterQueue(CmdCallbackQueueItem cmdQueueItem) {
        cmdCallbackQueue.enqueue(cmdQueueItem);
    }

    @Override
    public Job stopJob(String path, Integer buildNumber) {
        Job runningJob = find(path, buildNumber);
        NodeResult result = runningJob.getRootResult();

        if (result == null) {
            throw new NotFoundException("running job not found node result - " + path);
        }

        if (!result.isRunning()) {
            return runningJob;
        }

        // do not handle job since it is not in running status
        try {
            final HashSet<NodeStatus> skipStatus = Sets.newHashSet(SUCCESS, FAILURE, TIMEOUT);
            nodeResultService.updateStatus(runningJob, STOPPED, skipStatus);

            stopJob(runningJob);
        } catch (Throwable throwable) {
            String message = "stop job error - " + ExceptionUtil.findRootCause(throwable);
            LOGGER.traceMarker("stopJob", message);
            throw new IllegalParameterException(message);
        }

        return runningJob;
    }

    @Override
    public Job update(Job job) {
        jobDao.update(job);
        return job;
    }

    /**
     * Update job instance with new job status and boardcast JobStatusChangeEvent
     */
    @Override
    public void updateJobStatusAndSave(Job job, JobStatus newStatus) {
        JobStatus originStatus = job.getStatus();

        if (originStatus == newStatus) {
            jobDao.update(job);
            return;
        }

        //if job has finish not to update status
        if (Job.FINISH_STATUS.contains(originStatus)) {
            return;
        }
        job.setStatus(newStatus);
        jobDao.update(job);

        this.dispatchEvent(new JobStatusChangeEvent(this, job, originStatus, newStatus));
    }

    /**
     * Update job status by root node result
     */
    private NodeResult setJobStatusByRootResult(Job job) {
        NodeResult rootResult = nodeResultService.find(job.getNodePath(), job.getId());
        JobStatus newStatus = job.getStatus();

        if (rootResult.isFailure()) {
            newStatus = JobStatus.FAILURE;
        }

        if (rootResult.isSuccess()) {
            newStatus = JobStatus.SUCCESS;
        }

        if (rootResult.isStop()) {
            newStatus = JobStatus.STOPPED;
        }

        updateJobStatusAndSave(job, newStatus);
        return rootResult;
    }

    private Job find(Job job) {
        if (job == null) {
            throw new NotFoundException("Job is not found");
        }

        List<NodeResult> childrenResult = nodeResultService.list(job, true);
        job.setChildrenResult(childrenResult);
        return job;
    }

    /**
     * Update job status and delete agent session
     */
    private void stopJob(Job job) {
        setJobStatusByRootResult(job);
        cmdService.deleteSession(job);
    }

    @Override
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
    public void checkTimeoutTask() {
        if (!isJobTimeoutExecuteTimeout) {
            return;
        }

        LOGGER.trace("job timeout task start");

        // create session job timeout 6s time out
        ZonedDateTime finishZoneDateTime = ZonedDateTime.now().minusSeconds(jobExecuteTimeoutCreateSessionDuration);
        List<Job> jobs = jobDao.listForExpired(finishZoneDateTime, JobStatus.SESSION_CREATING);
        for (Job job : jobs) {
            updateJobAndNodeResultTimeout(job);
        }

        // running job timeout 1h time out
        ZonedDateTime finishRunningZoneDateTime = ZonedDateTime.now().minusSeconds(jobExecuteTimeoutRunningDuration);
        List<Job> runningJobs = jobDao.listForExpired(finishRunningZoneDateTime, JobStatus.RUNNING);
        for (Job job : runningJobs) {
            updateJobAndNodeResultTimeout(job);
        }

        LOGGER.trace("job timeout task end");
    }

    private void updateJobAndNodeResultTimeout(Job job) {
        // if job is running , please delete session first
        if (job.getStatus() == JobStatus.RUNNING) {
            try {
                cmdService.deleteSession(job);
            } catch (Throwable e) {
                LOGGER.warn(
                    "Error on delete session for job %s: %s",
                    job.getId(),
                    ExceptionUtil.findRootCause(e).getMessage());
            }
        }

        updateJobStatusAndSave(job, JobStatus.TIMEOUT);
        nodeResultService.updateStatus(job, NodeStatus.TIMEOUT, NodeResult.FINISH_STATUS);
    }

    private String logUrl(final Job job) {
        Path path = Paths.get("/", "jobs", job.getNodeName(), job.getNumber().toString(), "log", "download");
        return domain + path.toString();
    }
}
