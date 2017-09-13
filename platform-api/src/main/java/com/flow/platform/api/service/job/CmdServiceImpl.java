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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.exception.HttpException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Response for send cmd to control center
 *
 * @author yang
 */
@Service
public class CmdServiceImpl implements CmdService {

    private final static Logger LOGGER = new Logger(CmdService.class);

    @Autowired
    private PlatformURL platformURL;

    @Value(value = "${platform.zone}")
    private String zone;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public String createSession(Job job, Integer retry) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.CREATE_SESSION, null);
        cmdInfo.setWebhook(buildCmdWebhook(job));
        LOGGER.traceMarker("CreateSession", "job id - %s", job.getId());

        // create session
        Cmd cmd = sendToQueue(cmdInfo, retry);
        if (cmd == null) {
            throw new IllegalStatusException("Unable to create session since cmd return null");
        }

        if (Strings.isNullOrEmpty(cmd.getSessionId())) {
            throw new IllegalStatusException("Invalid session id");
        }

        return cmd.getSessionId();
    }

    /**
     * Delete job session
     */
    @Override
    public void deleteSession(Job job) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.DELETE_SESSION, null);
        cmdInfo.setSessionId(job.getSessionId());
        cmdInfo.setWebhook(buildCmdWebhook(job));

        try {
            LOGGER.traceMarker("DeleteSession", "Send delete session for session id %s", job.getSessionId());
            sendDirectly(cmdInfo);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warnMarker("DeleteSession", "Encoding error: %s", e.getMessage());
        }
    }

    @Override
    public CmdInfo runShell(Job job, Node node, String cmdId) {
        CmdInfo cmdInfo = new CmdInfo(zone, null, CmdType.RUN_SHELL, node.getScript());
        cmdInfo.setInputs(node.getEnvs());
        cmdInfo.setWebhook(buildCmdWebhook(job));
        cmdInfo.setOutputEnvFilter("FLOW_");
        cmdInfo.setSessionId(job.getSessionId());
        cmdInfo.setExtra(node.getPath()); // use cmd.extra to keep node path info
        cmdInfo.setCustomizedId(cmdId);

        try {
            LOGGER.traceMarker("RunShell", "step name - %s, node path - %s", node.getName(), node.getPath());
            sendDirectly(cmdInfo);
        } catch (Throwable e) {
            String rootCause = ExceptionUtil.findRootCause(e).getMessage();
            LOGGER.warnMarker("RunShell", "Unexpected exception: %s", rootCause);

            /// set cmd status to exception
            cmdInfo.setStatus(CmdStatus.EXCEPTION);
            cmdInfo.setExtra("Unexpected exception: " + rootCause);
        }

        return cmdInfo;
    }

    @Override
    public void shutdown(AgentPath path, String password) {
        CmdInfo cmdInfo = new CmdInfo(path, CmdType.SHUTDOWN, password);
        try {
            LOGGER.traceMarker("Shutdown", "Send shutdown for %s with pass '%s'", path, password);
            sendDirectly(cmdInfo);
        } catch (Throwable e) {
            String rootCause = ExceptionUtil.findRootCause(e).getMessage();
            throw new IllegalStatusException("Unable to shutdown since: " + rootCause);
        }
    }

    /**
     * Send cmd to control center directly
     */
    private Cmd sendDirectly(CmdInfo cmdInfo) throws UnsupportedEncodingException {
        String cmdUrl = platformURL.getCmdUrl();
        String res = HttpUtil.post(cmdUrl, cmdInfo.toJson());

        if (res == null) {
            LOGGER.warn("Error on send cmd: %s - %s", cmdUrl, cmdInfo);
            throw new HttpException(String.format("Error on send cmd: %s - %s", cmdInfo.getExtra(), cmdInfo));
        }

        return Jsonable.parse(res, Cmd.class);
    }

    /**
     * Send cmd to control center cmd queue
     */
    private Cmd sendToQueue(CmdInfo cmdInfo, Integer retry) {
        final StringBuilder stringBuilder = new StringBuilder(platformURL.getQueueUrl());
        stringBuilder.append("?priority=1&retry=").append(retry);

        try {
            String res = HttpUtil.post(stringBuilder.toString(), cmdInfo.toJson());

            if (res == null) {
                String message = String
                    .format("post session to queue error, cmdUrl: %s, cmdInfo: %s", stringBuilder, cmdInfo.toJson());

                LOGGER.warn(message);
                throw new HttpException(message);
            }

            return Jsonable.parse(res, Cmd.class);
        } catch (Throwable ignore) {
            LOGGER.warnMarker("SendToQueue", "Unexpected exception", ignore);
            return null;
        }
    }

    /**
     * Build cmd callback webhook url with job id as identifier
     */
    private String buildCmdWebhook(Job job) {
        return domain + "/hooks/cmd?identifier=" + HttpUtil.urlEncode(job.getId().toString());
    }
}
