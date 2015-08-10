package com.xl.rpc.cluster;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by issac on 15/8/10.
 */
public class ZKClient {

    private static ZooKeeper zkc;
    private static int Session_Timeout = 5*1000;

    static Watcher watcher = new Watcher(){
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                for (Watcher watcher:connectedWacther) {
                    watcher.process(event);
                }
            }
        }
    };

    static private String serverAddr;
    static private List<Watcher> connectedWacther = new ArrayList<>();

    static public void registerConnectedWatcher(Watcher watcher) {
        if (!connectedWacther.contains(watcher)) {
            connectedWacther.add(watcher);
            if (zkc != null && zkc.getState() == ZooKeeper.States.CONNECTED) {
                watcher.process(new WatchedEvent(Watcher.Event.EventType.None, Watcher.Event.KeeperState.SyncConnected,null));
            }
        }
    }

    static public ZooKeeper getZookeeper(String serverAddr) {
        if (zkc == null) {
            ZKClient.serverAddr = serverAddr;
            try {
                zkc = new ZooKeeper(serverAddr, Session_Timeout, watcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!serverAddr.equals(serverAddr)) {
                throw  new RuntimeException("zookeeper server addr must unique");
            }
        }
        return zkc;
    }
}
