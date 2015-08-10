package com.xl.rpc.cluster;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by issac on 15/8/10.
 */
public class ZKConfigSync {
    private static final Logger log = LoggerFactory.getLogger(ZKConfigSync.class);

    private ZooKeeper zkc;
    ConfigSyncListener listener;
    private static int Session_Timeout = 5 * 1000;

    private String clusterName;
    private String zkServerAddr;

    interface ConfigSyncListener {
        void onConfigChanged(String config);
    }

    private String config;
    private static String ConfigStore = "/config/";

    private ConfigWatcher configWatcher;



    public ZKConfigSync(String zkServer, String clusterName,ConfigSyncListener listener) {
        this.listener = listener;
        this.clusterName = clusterName;
        this.zkServerAddr = zkServer;
        configWatcher = new ConfigWatcher();
        try {
            zkc = new ZooKeeper(zkServer, Session_Timeout, watcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getConfig() {
        update();
        monitor();
        return config;
    }

    private String getPath() {
        return ConfigStore + clusterName;
    }


    Watcher watcher = new Watcher() {
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                getConfig();
            }
        }
    };

    void update() {
        byte[] data = null;
        try {
            data = zkc.getData(getPath(), null, null);
        } catch (Exception e) {
            e.printStackTrace();
            data = null;
        }
        if (data != null) {
            try {
                String config = new String(data, "utf-8");
                if (this.config == null && config != null ||
                        !this.config.equals(config)) {
                    this.config = config;
                    if (listener != null) {
                        listener.onConfigChanged(this.config);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    void monitor() {
        try {
            zkc.exists(getPath(),configWatcher);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ConfigWatcher implements Watcher {
        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.NodeCreated ||
                    watchedEvent.getType() == Event.EventType.NodeDataChanged ||
                    watchedEvent.getType() == Event.EventType.NodeDeleted) {
                String path = watchedEvent.getPath();
                if (getPath().equals(path)) {
                    getConfig();
                }
            }
        }
    }

}
