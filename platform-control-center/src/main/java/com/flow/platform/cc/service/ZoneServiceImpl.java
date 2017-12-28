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

import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.util.ZKHelper;
import com.flow.platform.cloud.InstanceManager;
import com.flow.platform.core.context.ContextEvent;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Instance;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author gy@fir.im
 */
@Service
public class ZoneServiceImpl implements ZoneService, ContextEvent {

    private final static Logger LOGGER = new Logger(ZoneService.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private CmdDispatchService cmdDispatchService;

    @Autowired
    private AgentSettings agentSettings;

    @Autowired
    private SpringContext springContext;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private List<Zone> defaultZones;

    @Autowired
    protected ZKClient zkClient;

    private final Map<Zone, ZoneEventListener> zoneEventWatchers = new HashMap<>();

    @Override
    public void start() {
        // init root node
        String path = createRoot();
        LOGGER.trace("Root zookeeper node initialized: %s", path);

        // init zone nodes
        for (Zone zone : defaultZones) {
            path = createZone(zone);
            LOGGER.trace("Zone node initialized: %s", path);
        }
    }

    @Override
    public void stop() {
        // ignore
    }

    @Override
    public String createRoot() {
        String rootPath = ZKHelper.buildPath(null, null);
        return zkClient.create(rootPath, null);
    }

    @Override
    public String createZone(Zone zone) {
        final String zonePath = ZKHelper.buildPath(zone.getName(), null);
        zone.setPath(zonePath);

        zkClient.create(zonePath, agentSettings.toBytes());

        List<String> agents = zkClient.getChildren(zonePath);

        if (!agents.isEmpty()) {
            for (String agent : agents) {
                agentService.report(new AgentPath(zone.getName(), agent), AgentStatus.IDLE, null);
            }
        }

        ZoneEventListener zoneEventWatcher = zoneEventWatchers.computeIfAbsent(zone, ZoneEventListener::new);
        zkClient.watchChildren(zonePath, zoneEventWatcher);
        return zonePath;
    }

    @Override
    public Zone getZone(String zoneName) {
        for (Zone zone : zoneEventWatchers.keySet()) {
            if (Objects.equals(zoneName, zone.getName())) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public List<Zone> getZones() {
        return Lists.newArrayList(zoneEventWatchers.keySet());
    }

    @Override
    public InstanceManager findInstanceManager(Zone zone) {
        if (!zone.isAvailable()) {
            return null;
        }
        String beanName = String.format("%sInstanceManager", zone.getCloudProvider());
        return (InstanceManager) springContext.getBean(beanName);
    }

    /**
     * Find num of idle agent and batch start instance
     *
     * @return boolean true = need start instance, false = has enough idle agent
     */
    @Override
    public boolean keepIdleAgentMinSize(final Zone zone, final InstanceManager instanceManager) {
        int numOfIdle = agentService.findAvailable(zone.getName()).size();
        LOGGER.traceMarker("keepIdleAgentMinSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle < zone.getMinPoolSize()) {
            instanceManager.batchStartInstance(zone);
            return true;
        }

        return false;
    }

    /**
     * Find num of idle agent and check max pool size,
     * send shutdown cmd to agent and delete instance
     */
    @Override
    public boolean keepIdleAgentMaxSize(final Zone zone, final InstanceManager instanceManager) {
        List<Agent> agentList = agentService.findAvailable(zone.getName());
        int numOfIdle = agentList.size();
        LOGGER.traceMarker("keepIdleAgentMaxSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle > zone.getMaxPoolSize()) {
            int numOfRemove = numOfIdle - zone.getMaxPoolSize();

            for (int i = 0; i < numOfRemove; i++) {
                Agent idleAgent = agentList.get(i);

                // send shutdown cmd
                Cmd shutdown = cmdService.create(new CmdInfo(idleAgent.getPath(), CmdType.SHUTDOWN, "flow.ci"));
                cmdDispatchService.dispatch(shutdown);
                LOGGER.traceMarker("keepIdleAgentMaxSize", "Send SHUTDOWN to idle agent: %s", idleAgent);

                // add instance to cleanup list
                Instance instance = instanceManager.find(idleAgent.getPath());
                if (instance != null) {
                    instanceManager.addToCleanList(instance);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = KEEP_IDLE_AGENT_TASK_PERIOD)
    public void keepIdleAgentTask() {
        if (!taskConfig.isEnableKeepIdleAgentTask()) {
            return;
        }

        LOGGER.traceMarker("keepIdleAgentTask", "start");

        // get num of idle agent
        for (Zone zone : getZones()) {
            InstanceManager instanceManager = findInstanceManager(zone);
            if (instanceManager == null) {
                continue;
            }

            if (keepIdleAgentMinSize(zone, instanceManager)) {
                continue;
            }

            keepIdleAgentMaxSize(zone, instanceManager);
        }

        LOGGER.traceMarker("keepIdleAgentTask", "end");
    }

    private class ZoneEventListener implements PathChildrenCacheListener {

        private final Zone zone;

        ZoneEventListener(Zone zone) {
            this.zone = zone;
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            final Type eventType = event.getType();
            final String path = event.getData().getPath();
            final String name = ZKHelper.getNameFromPath(path);

            String os = null;

            // get os when child node added
            if (eventType == Type.CHILD_ADDED) {
                try {
                    byte[] osData = client.getData().forPath(path);
                    os = Objects.isNull(osData) ? null : new String(osData);
                } catch (Exception e) {
                    // cannot get data of zk node
                }
            }

            if (eventType == Type.CHILD_ADDED || eventType == Type.CHILD_UPDATED) {
                LOGGER.debugMarker("ZoneEventListener", "Receive zookeeper event %s %s %s", eventType, path, os);
                agentService.report(new AgentPath(zone.getName(), name), AgentStatus.IDLE, os);
                return;
            }

            if (eventType == Type.CHILD_REMOVED) {
                LOGGER.debugMarker("ZoneEventListener", "Receive zookeeper event %s %s %s", eventType, path, null);
                agentService.report(new AgentPath(zone.getName(), name), AgentStatus.OFFLINE, null);
            }
        }
    }
}
