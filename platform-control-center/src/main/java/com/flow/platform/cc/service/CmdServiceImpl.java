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

package com.flow.platform.cc.service;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.cc.dao.CmdDao;
import com.flow.platform.cc.dao.CmdResultDao;
import com.flow.platform.cc.domain.CmdQueueItem;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.task.CmdWebhookTask;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.flow.platform.exception.FlowException;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.AbstractZkException;
import com.flow.platform.util.zk.ZkException.NotExitException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author gy@fir.im
 */
@Service(value = "cmdService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class CmdServiceImpl extends ZkServiceBase implements CmdService {

    private final static Logger LOGGER = new Logger(CmdService.class);

    @Value("${mq.queue.name}")
    private String cmdQueueName;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private Queue<Path> cmdLoggingQueue;

    @Autowired
    private CmdDao cmdDao;

    @Autowired
    private CmdResultDao cmdResultDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Cmd create(CmdInfo info) {
        String cmdId = UUID.randomUUID().toString();
        Cmd cmd = Cmd.convert(info);
        cmd.setId(cmdId);
        cmd.setCreatedDate(DateUtil.utcNow());
        cmd.setUpdatedDate(DateUtil.utcNow());

        // set default cmd timeout from zone setting if not defined
        if (info.getTimeout() == null) {
            Zone zone = zoneService.getZone(info.getZoneName());
            cmd.setTimeout(zone.getDefaultCmdTimeout());
        }

        cmdDao.save(cmd);
        return cmd;
    }

    @Override
    public Cmd find(String cmdId) {
        return cmdDao.get(cmdId);
    }

    @Override
    public List<Cmd> listByAgentPath(AgentPath agentPath) {
        return cmdDao.list(agentPath, null, null);
    }

    @Override
    public List<Cmd> listByZone(String zone) {
        return cmdDao.list(new AgentPath(zone, null), null, null);
    }

    @Override
    public List<CmdResult> listResult(Set<String> cmdIds) {
        return cmdResultDao.list(cmdIds);
    }

    @Override
    public boolean isTimeout(Cmd cmd) {
        if (cmd.getType() != CmdType.RUN_SHELL) {
            throw new IllegalParameterException("Check timeout only for run shell");
        }

        // not timeout since cmd is executed
        if (!cmd.isCurrent()) {
            return false;
        }

        ZonedDateTime createdAt = cmd.getCreatedDate();
        final long runningInSeconds = ChronoUnit.SECONDS.between(createdAt, ZonedDateTime.now());
        return runningInSeconds >= cmd.getTimeout();
    }

    @Override
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        noRollbackFor = {FlowException.class, AbstractZkException.class})
    public Cmd send(String cmdId, boolean shouldResetStatus) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalParameterException(String.format("Cmd '%s' does not exist", cmdId));
        }

        // verify input cmd status is in finished status
        if (!shouldResetStatus && !cmd.isCurrent()) {
            throw new IllegalStatusException(
                String.format("Cmd cannot be proceeded since status is: %s", cmd.getStatus()));
        }

        if (shouldResetStatus) {
            resetStatus(cmdId);
        }

        try {
            // find agent
            Agent target = selectAgent(cmd);

            // set cmd path and save since some of cmd not defined agent name
            cmd.setAgentPath(target.getPath());
            cmdDao.update(cmd);

            // double check agent in zk node
            String agentNodePath = zkHelper.getZkPath(target.getPath());
            if (ZkNodeHelper.exist(zkClient, agentNodePath) == null) {
                throw new AgentErr.NotFoundException(target.getPath().toString());
            }

            updateAgentStatusByCmdType(cmd, target);

            // set real cmd to zookeeper node
            if (cmd.isAgentCmd()) {
                ZkNodeHelper.setNodeData(zkClient, agentNodePath, cmd.toJson());
            }

            // update cmd status to SENT
            updateStatus(cmd.getId(), CmdStatus.SENT, null, false, true);
            return cmd;

        } catch (AgentErr.NotAvailableException e) {
            updateStatus(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            zoneService.keepIdleAgentTask();
            throw e;

        } catch (NotExitException e) {
            updateStatus(cmd.getId(), CmdStatus.REJECTED, null, false, true);
            throw new AgentErr.NotFoundException(cmd.getAgentName());

        } catch (Throwable e) {
            CmdResult result = new CmdResult();
            result.getExceptions().add(e);
            updateStatus(cmd.getId(), CmdStatus.EXCEPTION, result, false, true);
            throw e;
        }
    }

    @Override
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        noRollbackFor = {FlowException.class, AbstractZkException.class})
    public Cmd send(CmdInfo cmdInfo) {
        Cmd cmd = create(cmdInfo);
        return send(cmd.getId(), false);
    }

    @Override
    public Cmd queue(CmdInfo cmdInfo, int priority, int retry) {
        Cmd cmd = create(cmdInfo);

        CmdQueueItem item = new CmdQueueItem(cmd.getId(), priority, retry);
        MessageProperties properties = new MessageProperties();
        properties.setPriority(item.getPriority());
        rabbitTemplate.send("", cmdQueueName, new Message(item.toBytes(), properties));

        return cmd;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateStatus(
        String cmdId, CmdStatus status, CmdResult inputResult, boolean updateAgentStatus, boolean callWebhook) {

        LOGGER.trace("Report cmd %s status %s and result %s", cmdId, status, inputResult);

        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd does not exist");
        }

        CmdResult cmdResult = cmdResultDao.get(cmd.getId());
        // compare exiting cmd result and update
        if (inputResult != null) {
            inputResult.setCmdId(cmdId);
            cmd.setFinishedDate(inputResult.getFinishTime());
            if (cmdResult != null) {
                cmdResultDao.updateNotNullOrEmpty(inputResult);
            } else {
                cmdResultDao.save(inputResult);
            }
        }
        cmd.setCmdResult(cmdResult);
        // update cmd status
        if (cmd.addStatus(status)) {
            cmdDao.update(cmd);

            // update agent status
            if (updateAgentStatus) {
                updateAgentStatusWhenUpdateCmd(cmd);
            }

            if (callWebhook) {
                webhookCallback(cmd);
            }
        }
    }

    @Override
    public void resetStatus(String cmdId) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd does not exist");
        }

        cmd.setStatus(CmdStatus.PENDING);
        cmdDao.save(cmd);
    }

    @Override
    public void saveLog(String cmdId, MultipartFile file) {
        Cmd cmd = find(cmdId);
        if (cmd == null) {
            throw new IllegalArgumentException("Cmd not exist");
        }

        try {
            Path target = Paths.get(AppConfig.CMD_LOG_DIR.toString(), file.getOriginalFilename());
            Files.write(target, file.getBytes());
            cmd.getLogPaths().add(target.toString());
            cmdDao.update(cmd);
            cmdLoggingQueue.add(target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void webhookCallback(CmdBase cmdBase) {
        if (cmdBase == null || cmdBase.getWebhook() == null) {
            return;
        }

        taskExecutor.execute(new CmdWebhookTask(cmdBase));
    }

    @Scheduled(fixedDelay = 300 * 1000)
    public void checkTimeoutTask() {
        if (!taskConfig.isEnableCmdExecTimeoutTask()) {
            return;
        }
        LOGGER.traceMarker("checkTimeoutTask", "start");

        // find all running status cmd
        List<Cmd> cmds = cmdDao.list(null, Sets.newHashSet(CmdType.RUN_SHELL), Cmd.WORKING_STATUS);

        for (Cmd cmd : cmds) {
            if (isTimeout(cmd)) {
                // kill current running cmd and report status
                send(new CmdInfo(cmd.getAgentPath(), CmdType.KILL, null));
                LOGGER.traceMarker("checkTimeoutTask", "Send KILL for timeout cmd %s", cmd);
                updateStatus(cmd.getId(), CmdStatus.TIMEOUT_KILL, null, true, true);
            }
        }

        LOGGER.traceMarker("checkTimeoutTask", "end");
    }

    /**
     * Select agent by AgentPath or session id
     * - auto select agent if only defined zone name
     *
     * @return Agent or null
     * @throws AgentErr.NotAvailableException no idle agent in zone
     * @throws AgentErr.AgentMustBeSpecified name must for operation cmd type
     * @throws AgentErr.NotFoundException target agent not found
     */
    private Agent selectAgent(CmdBase cmd) {
        // check session id as top priority
        if (cmd.hasSession()) {
            Agent target = agentService.find(cmd.getSessionId());
            if (target == null) {
                throw new AgentErr.NotFoundException(cmd.getSessionId());
            }
            return target;
        }

        // verify agent path is presented
        AgentPath agentPath = cmd.getAgentPath();
        if (isAgentPathFail(cmd, agentPath)) {
            throw new AgentErr.AgentMustBeSpecified();
        }

        // auto select agent inside zone
        if (agentPath.getName() == null) {
            List<Agent> availableList = agentService.findAvailable(agentPath.getZone());
            if (availableList.size() > 0) {
                Agent target = availableList.get(0);
                cmd.setAgentPath(target.getPath()); // reset cmd path
                return target;
            }

            throw new AgentErr.NotAvailableException(agentPath.getZone() + ":null");
        }

        // find agent by path
        Agent target = agentService.find(agentPath);
        if (target == null) {
            throw new AgentErr.NotFoundException(cmd.getAgentName());
        }

        return target;
    }

    /**
     * Update agent status by cmd type
     *
     * @param cmd cmd instance created by cmdInfo
     * @param target target agent that needs to set status
     */
    private void updateAgentStatusByCmdType(final Cmd cmd, final Agent target) {
        switch (cmd.getType()) {
            case RUN_SHELL:
                if (cmd.hasSession()) {
                    break;
                }

                // add reject status since busy
                if (!target.isAvailable()) {
                    throw new AgentErr.NotAvailableException(target.getName());
                }

                target.setStatus(AgentStatus.BUSY);
                break;

            case CREATE_SESSION:
                // add reject status since unable to create session for agent
                String sessionId = agentService.createSession(target);
                if (sessionId == null) {
                    throw new AgentErr.NotAvailableException(target.getName());
                }

                // set session id to cmd and save
                cmd.setSessionId(sessionId);
                cmdDao.update(cmd);

                break;

            case DELETE_SESSION:
                agentService.deleteSession(target);
                break;

            case KILL:
                // DO NOT handle it, agent status from cmd update
                break;

            case STOP:
                agentService.deleteSession(target);
                target.setStatus(AgentStatus.OFFLINE);
                break;

            case SHUTDOWN:
                // in shutdown action, cmd content is sudo password
                if (Strings.isNullOrEmpty(cmd.getCmd())) {
                    throw new IllegalParameterException(
                        "For SHUTDOWN action, password of 'sudo' must be provided");
                }

                agentService.deleteSession(target);
                target.setStatus(AgentStatus.OFFLINE);
                break;
        }

        // update agent property
        agentDao.update(target);
    }

    private boolean isAgentPathFail(CmdBase cmd, AgentPath agentPath) {
        if (cmd.getType() == CmdType.CREATE_SESSION || cmd.getType() == CmdType.DELETE_SESSION) {
            return false;
        }
        return agentPath.getName() == null && cmd.getType() != CmdType.RUN_SHELL;
    }

    /**
     * Update agent status when report cmd status and result
     * - busy or idle by Cmd.Type.RUN_SHELL while report cmd status
     *
     * @param cmd Cmd object
     */
    private void updateAgentStatusWhenUpdateCmd(Cmd cmd) {
        // do not update agent status duration session
        String sessionId = cmd.getSessionId();
        if (sessionId != null && agentService.find(sessionId) != null) {
            return;
        }

        // update agent status by cmd status
        AgentPath agentPath = cmd.getAgentPath();
        boolean isAgentBusy = false;
        for (Cmd tmp : listByAgentPath(agentPath)) {
            if (tmp.getType() != CmdType.RUN_SHELL) {
                continue;
            }

            if (!tmp.getAgentPath().equals(agentPath)) {
                continue;
            }

            if (tmp.isCurrent()) {
                isAgentBusy = true;
                break;
            }
        }

        agentService.updateStatus(agentPath, isAgentBusy ? AgentStatus.BUSY : AgentStatus.IDLE);
    }
}