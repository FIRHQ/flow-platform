package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public interface CmdService {

    /**
     * Create command from CmdBase
     *
     * @param cmd
     * @return Cmd objc with id
     */
    Cmd create(CmdInfo cmd);

    /**
     * Find cmd obj by id
     *
     * @param cmdId
     * @return Cmd object
     */
    Cmd find(String cmdId);

    /**
     * List cmd by agent path, order by cmd create date
     *
     * @param agentPath
     * @return
     */
    List<Cmd> listByAgentPath(AgentPath agentPath);

    /**
     * List cmd by zone, order by cmd create date
     *
     * @param zone
     * @return
     */
    List<Cmd> listByZone(String zone);

    /**
     * List cmd result by ids
     *
     * @param cmdIds
     * @return
     */
    List<CmdResult> listResult(Set<String> cmdIds);

    /**
     * Send CmdBase with AgentPath which to identify where is cmd destination
     *  - AgentPath,
     *   - 'zone' field is required
     *   - 'name' field is optional
     *      - which mean system will automatic select idle agent to send
     *        throw AgentErr.NotAvailableException if no idle agent
     *
     * @param cmdInfo
     * @return command objc with id
     * @exception AgentErr.NotAvailableException if agent busy
     */
    Cmd send(CmdInfo cmdInfo);

    /**
     * Check cmd is timeout
     *
     * @param cmd
     * @return timeout or not
     */
    boolean isTimeout(Cmd cmd);

    /**
     * Update cmd status and result
     *
     * @param cmdId  target cmd id
     * @param status target cmd status should updated
     * @param result CmdResult, nullable
     * @param updateAgentStatus should update agent status according to cmd status
     */
    void updateStatus(String cmdId, CmdStatus status, CmdResult result, boolean updateAgentStatus);

    /**
     * Record full zipped log to store
     *
     * @param cmdId
     * @param file
     */
    void saveLog(String cmdId, MultipartFile file);

    /**
     * Invoke webhook url to report Cmd
     *
     * @param cmdBase
     */
    void webhookCallback(CmdBase cmdBase);

    /**
     * Check timeout cmd by created date for all busy agent
     */
    void checkTimeoutTask();
}
