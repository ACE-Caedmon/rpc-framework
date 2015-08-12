package com.xl.rpc.cluster.zookeeper;

import com.xl.rpc.cluster.ZKConfigSync;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/8/11.
 */
public class ZkConfigWatcher implements Watcher{
    private static final Logger log= LoggerFactory.getLogger(ZkConfigWatcher.class);
    private ZKConfigSync zkConfigSync;

    public ZkConfigWatcher(ZKConfigSync zkConfigSync){
        this.zkConfigSync=zkConfigSync;
    }
    public void process(WatchedEvent watchedEvent) {
        log.info("ConfigWatcher process {},path {}", watchedEvent,zkConfigSync.getPath());
        if (watchedEvent.getType() == Event.EventType.NodeCreated ||
                watchedEvent.getType() == Event.EventType.NodeDataChanged ||
                watchedEvent.getType() == Event.EventType.NodeDeleted) {
            String path = watchedEvent.getPath();
            if (zkConfigSync.getPath().equals(path)) {
                zkConfigSync.updateConfig();
            }
        } else {
            zkConfigSync.monitor();
        }
    }
}
