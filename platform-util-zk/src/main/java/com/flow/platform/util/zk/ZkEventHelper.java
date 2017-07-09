package com.flow.platform.util.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class ZkEventHelper {

    /**
     * Client connect to server event
     *
     * @return is connected to zookeeper server
     */
    public static boolean isConnectToServer(WatchedEvent event) {
        return syncConnectedEvent(event, EventType.None);
    }

    public static boolean isDisconnected(WatchedEvent event) {
        return event.getState() == Watcher.Event.KeeperState.Disconnected;
    }

    /**
     * Indicate is zk node created
     *
     * @return zookeeper node is created
     */
    public static boolean isCreated(WatchedEvent event) {
        return syncConnectedEvent(event, EventType.NodeCreated);
    }

    /**
     * Indicate is zk node created for path
     *
     * @return zookeeper node on specific path is created
     */
    public static boolean isCreatedOnPath(WatchedEvent event, String path) {
        if (isCreated(event)) {
            return isMatchPath(event, path);
        }
        return false;
    }

    /**
     * Node deleted event
     *
     * @return zookeeper node is deleted
     */
    public static boolean isDeleted(WatchedEvent event) {
        return syncConnectedEvent(event, EventType.NodeDeleted);
    }

    public static boolean isDeletedOnPath(WatchedEvent event, String path) {
        if (isDeleted(event)) {
            return isMatchPath(event, path);
        }
        return false;
    }

    public static boolean isDataChanged(WatchedEvent event) {
        return syncConnectedEvent(event, EventType.NodeDataChanged);
    }

    public static boolean isDataChangedOnPath(WatchedEvent event, String path) {
        if (isDataChanged(event)) {
            return isMatchPath(event, path);
        }
        return false;
    }

    public static boolean isChildrenChanged(WatchedEvent event) {
        return syncConnectedEvent(event, EventType.NodeChildrenChanged);
    }

    public static boolean isChildrenChangedOnPath(WatchedEvent event, String path) {
        if (isChildrenChanged(event)) {
            return isMatchPath(event, path);
        }
        return false;
    }

    public static boolean isSessionExpired(WatchedEvent event) {
        return event.getState() == Watcher.Event.KeeperState.Expired;
    }

    private static boolean syncConnectedEvent(WatchedEvent event, EventType eventType) {
        return event.getState() == Watcher.Event.KeeperState.SyncConnected && event.getType() == eventType;
    }

    private static boolean isMatchPath(WatchedEvent event, String targetPath) {
        return event.getPath() != null && event.getPath().equals(targetPath);
    }
}
