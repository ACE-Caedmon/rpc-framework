package com.xl.rpc.cluster;

import com.xl.rpc.cluster.zookeeper.ZkConfigWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by issac on 15/8/10.
 */
public class ZKConfigSync {
    private static final Logger log = LoggerFactory.getLogger(ZKConfigSync.class);
    ConfigSyncListener listener;

    private String clusterName;
    private String zkServerAddr;
    public interface ConfigSyncListener {
        void onConfigChanged(String config);
    }

    private String config;
    private static String ConfigStore = "/config/";

    private ZkConfigWatcher configWatcher;

    private ScheduledExecutorService pullConfigTimer= Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread=new Thread(r);
            thread.setName("Pull-ZkConfig-Timer");
            return thread;
        }
    });

    public ZKConfigSync(String zkServer, String clusterName,ConfigSyncListener listener) {
        this.listener = listener;
        this.clusterName = clusterName;
        this.zkServerAddr = zkServer;
        this.configWatcher= new ZkConfigWatcher(this);
        ZKClient.getInstance().registerConnectedWatcher(watcher);
        pullConfigTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try{
                    updateConfig();
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("Update config error");
                }

            }
        },0,1, TimeUnit.MINUTES);
    }

    Watcher watcher = new Watcher() {
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            log.info("ZKConfigSync process {} config {}",event,clusterName);
            if (event.getState() == Event.KeeperState.SyncConnected) {
                updateConfig();
            }
        }
    };

    public String getConfig() {
        return config;
    }


    public synchronized void updateConfig() {
        String oldConfig = config;
        update();
        monitor();

        if (config == null && oldConfig == null ||
                config.equals(oldConfig)) {

        } else {
            log.info("cluster {} oldConfig {} newConfig {}",clusterName,oldConfig,config);
            if (listener != null) {
                listener.onConfigChanged(config);
            }
        }
    }

    public String getPath() {
        return ConfigStore + clusterName;
    }

    private void update() {
        byte[] data = null;
        try {
            if (getZookeeper().exists(getPath(), null) != null) {
                data = getZookeeper().getData(getPath(), null, null);
            } else {
                this.config = null;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            data = null;
        }
        if (data != null) {
            try {
                String config = new String(data, "utf-8");
                this.config = config;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void monitor() {
        try {
            getZookeeper().exists(getPath(), configWatcher);
        } catch (Exception e) {
            log.warn("monitor fail",e);
        }
    }

    public ZooKeeper getZookeeper(){
        return ZKClient.getInstance().getZookeeper(zkServerAddr);
    }

}
