package com.flow.platform.cc.pool;

import com.flow.platform.util.mos.Instance;
import com.flow.platform.util.mos.MosClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Meituan cloud pool manager
 * <p>
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
@Component(value = "mosPoolManager")
public class MosPoolManager implements PoolManager {

    @Value("${mos.key}")
    private String mosKey;

    @Value("${mos.secret}")
    private String mosSecret;

    @Value("${mos.image}")
    private String mosImage;

    @Value("${mos.instance_name_pattern}")
    private String mosInstanceNamePattern;

    @Autowired
    private Executor taskExecutor;

    private MosClient mosClient;

    // running mos instance
    private final Map<String, Instance> mosRunningQueue = new ConcurrentHashMap<>();

    // failed mos instances
    private final Map<String, Instance> mosFailureQueue = new ConcurrentHashMap<>();

    @PostConstruct
    public void init () throws Throwable {
        mosClient = new MosClient(mosKey, mosSecret);
    }

    @Override
    public Collection<Instance> runningInstance() {
        return mosRunningQueue.values();
    }

    @Override
    public Collection<Instance> failureInstance() {
        return mosFailureQueue.values();
    }

    @Override
    public List<String> batchStartInstance(int numOfInstance) {
        List<String> expectNameList = new ArrayList<>(numOfInstance);
        for (int i = 0; i < numOfInstance; i ++) {
            String instanceName = String.format(mosInstanceNamePattern, UUID.randomUUID());
            taskExecutor.execute(new StartMosInstanceWorker(mosClient, mosImage, instanceName));
            expectNameList.add(instanceName);
        }
        return expectNameList;
    }

    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void deleteFailureInstance() {
        cleanRunningInstance(mosFailureQueue);
    }

    @Override
    public void clean() {
        deleteFailureInstance();
        cleanRunningInstance(mosRunningQueue);
    }

    private void cleanRunningInstance(Map<String, Instance> instanceMap) {
        Iterator<Map.Entry<String, Instance>> iterator = instanceMap.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, Instance> entry = iterator.next();
            Instance mosInstance = entry.getValue();
            mosClient.deleteInstance(mosInstance.getInstanceId());
            iterator.remove();
        }
    }

    /**
     * Thread to start zone instance
     */
    private class StartMosInstanceWorker implements Runnable {

        private final MosClient mosClient;
        private final String imageName;
        private final String instanceName;

        private StartMosInstanceWorker(MosClient mosClient, String imageName, String instanceName) {
            this.mosClient = mosClient;
            this.imageName = imageName;
            this.instanceName = instanceName;
        }

        @Override
        public void run() {
            Instance instance = null;
            try {
                instance = mosClient.createInstance(imageName, instanceName);

                // wait instance status to running with 30 seconds timeout
                if (mosClient.instanceStatusSync(instance.getInstanceId(), Instance.STATUS_RUNNING, 30 * 1000)) {
                    System.out.println(instance);
                    mosRunningQueue.put(instanceName, instance);
                } else {
                    mosFailureQueue.put(instanceName, instance);
                }
            } catch (Throwable e) {
                // TODO: should add logging
                if (instance != null) {
                    mosFailureQueue.put(instanceName, instance);
                }
            }
        }
    }
}
