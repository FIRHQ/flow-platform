package com.flow.platform.cc.service;

import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.zk.ZkException;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.flow.platform.util.zk.ZkPathBuilder;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 24/05/2017.
 * Copyright fir.im
 */
@Service(value = "agentService")
public class AgentServiceImpl implements AgentService {

    private final Map<String, Map<AgentPath, Agent>> agentOnlineList = new HashMap<>();

    private final ReentrantLock onlineListUpdateLock = new ReentrantLock();

    @Autowired
    private ZkService zkService;

    @Override
    public void reportOnline(String zone, Collection<AgentPath> keys) {
        onlineListUpdateLock.lock();
        try {
            Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());

            // find offline agent
            HashSet<AgentPath> offlines = new HashSet<>(agentList.keySet());
            offlines.removeAll(keys);

            // remote from online list and update status
            for (AgentPath key : offlines) {
                Agent offlineAgent = agentList.get(key);
                offlineAgent.setStatus(Agent.Status.OFFLINE);
                agentList.remove(key);
            }

            // fine newly online agent
            HashSet<AgentPath> onlines = new HashSet<>(keys);
            onlines.removeAll(agentList.keySet());

            // report online
            for (AgentPath key : onlines) {
                reportOnline(key);
            }
        } finally {
            onlineListUpdateLock.unlock();
        }
    }

    @Override
    public Agent find(AgentPath key) {
        String zone = key.getZone();
        Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
        return agentList.get(key);
    }

    @Override
    public Collection<Agent> onlineList(String zone) {
        Collection<Agent> zoneAgents = new ArrayList<>(agentOnlineList.size());
        Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
        for (Agent agent : agentList.values()) {
            if (agent.getZone().equals(zone)) {
                zoneAgents.add(agent);
            }
        }
        return zoneAgents;
    }

    @Override
    public void reportStatus(AgentPath path, Agent.Status status) {
        Agent exist = find(path);
        if (exist == null) {
            throw new AgentErr.NotFoundException(path.getName());
        }
        exist.setStatus(status);
    }

    /**
     * Find from online list and create
     *
     * @param key
     */
    private void reportOnline(AgentPath key) {
        Agent exist = find(key);
        if (exist == null) {
            String zone = key.getZone();
            Map<AgentPath, Agent> agentList = agentOnlineList.computeIfAbsent(zone, k -> new HashMap<>());
            Agent agent = new Agent(key);
            agent.setStatus(Agent.Status.IDLE);
            agentList.put(key, agent);
        }
    }
}
