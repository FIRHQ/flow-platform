package com.flow.platform.cc.service;

import com.flow.platform.cc.cloud.InstanceManager;
import com.flow.platform.cc.config.TaskConfig;
import com.flow.platform.cc.util.SpringContextUtil;
import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.domain.*;
import com.flow.platform.util.Logger;
import com.flow.platform.util.mos.Instance;
import com.flow.platform.util.zk.ZkEventHelper;
import com.flow.platform.util.zk.ZkNodeHelper;
import com.google.common.collect.Lists;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@Service(value = "zoneService")
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class ZoneServiceImpl extends ZkServiceBase implements ZoneService {

    private final static Logger LOGGER = new Logger(ZoneService.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private Executor taskExecutor;

    @Autowired
    private SpringContextUtil springContextUtil;

    @Autowired
    private TaskConfig taskConfig;

    @Autowired
    private CountDownLatch initLatch;

    private final Map<Zone, ZoneEventWatcher> zoneEventWatchers = new HashMap<>();

    @PostConstruct
    private void init() {
        // init root node and watch children event
        String rootPath = zkHelper.buildZkPath(null, null).path();
        ZkNodeHelper.createNode(zkClient, rootPath, "");

        // init zone nodes via thread, should wait agent service initialized
        taskExecutor.execute(() -> {
            try {
                initLatch.await();
                for (Zone zone : zkHelper.getZones()) {
                    createZone(zone);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for agent service initialized", e);
            }
        });
    }

    @Override
    public String createZone(Zone zone) {
        final String zonePath = zkHelper.buildZkPath(zone.getName(), null).path();

        // zone node not exited
        if (ZkNodeHelper.exist(zkClient, zonePath) == null){
            ZkNodeHelper.createNode(zkClient, zonePath, agentConfig.toJson());
        } else{
            ZkNodeHelper.setNodeData(zkClient, zonePath, agentConfig.toJson());
            List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
            agentService.reportOnline(zone.getName(), buildKeys(zone.getName(), agents));
        }

        ZoneEventWatcher zoneEventWatcher =
                zoneEventWatchers.computeIfAbsent(zone, z -> new ZoneEventWatcher(z, zonePath));

        ZkNodeHelper.watchChildren(zkClient, zonePath, zoneEventWatcher, 5);
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
        String beanName = String.format("%sInstanceManager", zone.getCloudProvider());
        return (InstanceManager) springContextUtil.getBean(beanName);
    }

    /**
     * Find num of idle agent and batch start instance
     *
     * @param zone
     * @param instanceManager
     * @return boolean
     *          true = need start instance,
     *          false = has enough idle agent
     */
    @Override
    public synchronized boolean keepIdleAgentMinSize(Zone zone, InstanceManager instanceManager, int minPoolSize) {
        int numOfIdle = agentService.findAvailable(zone.getName()).size();
        LOGGER.traceMarker("keepIdleAgentMinSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle < minPoolSize) {
            instanceManager.batchStartInstance(minPoolSize);
            return true;
        }

        return false;
    }

    /**
     * Find num of idle agent and check max pool size,
     * send shutdown cmd to agent and delete instance
     *
     * @param zone
     * @param instanceManager
     * @return
     */
    @Override
    public synchronized boolean keepIdleAgentMaxSize(Zone zone, InstanceManager instanceManager, int maxPoolSize) {
        List<Agent> agentList = agentService.findAvailable(zone.getName());
        int numOfIdle = agentList.size();
        LOGGER.traceMarker("keepIdleAgentMaxSize", "Num of idle agent in zone %s = %s", zone, numOfIdle);

        if (numOfIdle > maxPoolSize) {
            int numOfRemove = numOfIdle - maxPoolSize;

            for (int i = 0; i < numOfRemove; i++) {
                Agent idleAgent = agentList.get(i);

                // send shutdown cmd
                CmdInfo cmdInfo = new CmdInfo(idleAgent.getPath(), CmdType.SHUTDOWN, "flow.ci");
                cmdService.send(cmdInfo);
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

            if (keepIdleAgentMinSize(zone, instanceManager, MIN_IDLE_AGENT_POOL)) {
                continue;
            }

            keepIdleAgentMaxSize(zone, instanceManager, MAX_IDLE_AGENT_POOL);
        }

        LOGGER.traceMarker("keepIdleAgentTask", "end");
    }

    private Collection<AgentPath> buildKeys(String zone, Collection<String> agents) {
        ArrayList<AgentPath> keys = new ArrayList<>(agents.size());
        for (String agentName : agents) {
            keys.add(new AgentPath(zone, agentName));
        }
        return keys;
    }

    /**
     * To handle zk events on zone level
     */
    private class ZoneEventWatcher implements Watcher {

        private final Zone zone;
        private final String zonePath;

        ZoneEventWatcher(Zone zone, String zonePath) {
            this.zone = zone;
            this.zonePath = zonePath;
        }

        public void process(WatchedEvent event) {
            zkHelper.recordEvent(zonePath, event);
            LOGGER.traceMarker("ZookeeperZoneEventHandler", "Zookeeper event received %s", event.toString());

            // continue to watch zone path
            ZkNodeHelper.watchChildren(zkClient, zonePath, this, 5);

            if (ZkEventHelper.isChildrenChanged(event)) {
                taskExecutor.execute(() -> {
                    List<String> agents = ZkNodeHelper.getChildrenNodes(zkClient, zonePath);
                    agentService.reportOnline(zone.getName(), buildKeys(zone.getName(), agents));
                });
            }
        }
    }
}
