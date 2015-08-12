package com.xl.rpc.cluster.zookeeper;

import com.xl.rpc.cluster.ZKClient;
import com.xl.rpc.cluster.ZkServiceDiscovery;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Administrator on 2015/8/11.
 */
public class ZkPathWatcher implements Watcher{
    private ZkServiceDiscovery zkServiceDiscovery;
    private static final Logger log= LoggerFactory.getLogger(ZkPathWatcher.class);
    String path;
    public ZkPathWatcher(ZkServiceDiscovery zkServiceDiscovery,String path) {
        this.path = path;
        this.zkServiceDiscovery=zkServiceDiscovery;
    }
    public void monitor() {
        ZooKeeper zooKeeper=zkServiceDiscovery.getZookeeper();
        zkServiceDiscovery.createPath(path);
        try {
            zooKeeper.getChildren(path, this);
        } catch (Exception e) {
            log.warn("monitor fail",e);
        }
    }

    public void process(WatchedEvent watchedEvent) {
        log.info("PathWatcher process {} path {}",watchedEvent,path);
        if (watchedEvent.getType() == Event.EventType.NodeCreated ||
                watchedEvent.getType() == Event.EventType.NodeDataChanged ||
                watchedEvent.getType() == Event.EventType.NodeDeleted ||
                watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
            String path = watchedEvent.getPath();
            if (ZkServiceDiscovery.ServerStore.equals(path)) {
                zkServiceDiscovery.updateAll();
            } else {
                try{
                    int index = path.lastIndexOf("/");
                    String service = path.substring(index + 1);
                    List<String> providers = zkServiceDiscovery.getServerListByPath(path);
                    zkServiceDiscovery.saveProviders(service, providers);
                }catch (Exception e){
                    log.error("Zookeeper process WatchEvent error:server = {}", path, e);
                }
            }
        }
        monitor();
    }
}
