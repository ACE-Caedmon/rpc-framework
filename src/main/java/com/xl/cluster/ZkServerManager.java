package com.xl.cluster;

import com.xl.exception.ClusterException;
import com.xl.utils.Util;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zwc on 2015/7/10.
 */
public class ZkServerManager {

    private ZooKeeper zkc;
    ServerConfigListener listener;

    private String clusterName;
    private List<String> monitorList;
    private static String ServerStore = "/server";
    private Map<String,PathWatcher> providerWatchers = new HashMap<String, PathWatcher>();
    private static final Logger log= LoggerFactory.getLogger(ZkServerManager.class);
    private static int Session_Timeout = 5*1000;
    static class CacheData {
        public Map<String,List<String>> providerMap = new HashMap<String, List<String>>(); // 需要lock保护
    }

    private Lock lock = new ReentrantLock();

    private CacheData cacheData = new CacheData();

    public interface ServerConfigListener {
        void onServerListChanged(String server);
    }

    Watcher watcher = new Watcher(){
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                update();
            }
        }
    };

    public ZkServerManager(String zkServer) {
        try {
            zkc = new ZooKeeper(zkServer, Session_Timeout, watcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /*
    * monitorServerGroups: 监听其他服务器的实例状态
    */
    public void monitorServiceProviders(List<String> monitorServerList) throws Exception{
        this.monitorList = monitorServerList;
        initNameServiceWatchers();
        update();
    }

    /*
    * logicName:logicName
    * address:ip:port
    */
    public void registerService(String logicName,String address) throws Exception{
        if (Util.isEmpty(address) || Util.isEmpty(logicName)) {
            throw new RuntimeException("ClusterName or server address is null");
        }
        this.clusterName = logicName;
        createPath(getSvrTreePath());
        String path = getSvrTreePath() + "/" + address;
        Stat stat = zkc.exists(path,false);
        // 说明程序挂了，立马又被拉起，这时候需要等zk服务器超时了再注册
        if (stat != null){
            Thread.sleep((long) (Session_Timeout * 1.5));
        }
        zkc.create(path, clusterName.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        log.debug("Register cluster success:clusterName = {}",logicName);
        update();
    }

    public void setListener(ServerConfigListener listener) {
        this.listener = listener;
    }





    private void initNameServiceWatchers() throws KeeperException,InterruptedException{
        if (monitorList != null) {
            for (String svrName : monitorList) {
                String path = ServerStore + "/" + svrName;
                if (!providerWatchers.containsKey(svrName)) {
                    PathWatcher pathWatcher = new PathWatcher(path);
                    providerWatchers.put(svrName,pathWatcher);
                }
                PathWatcher pathWatcher = providerWatchers.get(svrName);
                pathWatcher.monitor();

            }
        }
    }


    synchronized void update() throws ClusterException{
        if (zkc == null)
            return;
        try{
            createPath(ServerStore);
            // query provider list
            if (monitorList != null) {
                for (String svrName : monitorList) {
                    List<String> nodes = getServerListByPath(ServerStore+"/"+svrName);
                    cacheData.providerMap.put(svrName,nodes);
                }
            }

            //  monitor provider server list
            if (monitorList != null) {
                for (String svrName : monitorList) {
                    PathWatcher pathWatcher = providerWatchers.get(svrName);
                    if(pathWatcher!=null){
                        pathWatcher.monitor();
                    }

                }
            }
        }catch (Exception e){
            throw new ClusterException("Update zookeeper nodes error",e);
        }

    }


    public void close() throws InterruptedException{
        zkc.close();
    }



    public Map<String,List<String>> getAllServerMap(){
        update();
        return cacheData.providerMap;
    }

    public List<String> getServerList(String svrName) {
        update();
        if (cacheData.providerMap.containsKey(svrName)) {
            return cacheData.providerMap.get(svrName);
        }

        return cacheData.providerMap.get(svrName);
    }

    private List<String> getServerListByPath(String path) throws KeeperException, InterruptedException {
        List<String> children = zkc.getChildren(path,false);
        return children;
    }


    private String getSvrTreePath() {
        String path = ServerStore + "/" + clusterName;
        return path;
    }


    private void createPath(String path) throws KeeperException, InterruptedException {
        Stat stat = zkc.exists(path, false);
        if (stat == null) {
            zkc.create(path, "".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }


    // path monitor
    class PathWatcher implements Watcher {
        String path;
        public PathWatcher(String path) {
            this.path = path;
        }
        public void monitor() throws KeeperException, InterruptedException {
            createPath(path);
            zkc.getChildren(path, this);
        }

        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.None && watchedEvent.getState() == Event.KeeperState.Disconnected)
                return;
            String path = watchedEvent.getPath();
            List<String> providers = null;
            if(path==null){
                log.warn("Zookeeper WatchEvent path null");
                return;
            }
            int index = path.lastIndexOf("/");
            String service = path.substring(index + 1);
            try{
                monitor();
                providers = getServerListByPath(path);
                saveProviders(service, providers);
            }catch (Exception e){
                e.printStackTrace();
                log.error("Zookeeper process WatchEvent error:server = {}", path, e);
            }


        }
    }

    private void notifyServerListChange(String svrName) {
        if (listener != null) {
            listener.onServerListChanged(svrName);
        }
    }


    private void saveProviders(String svr,List<String> providers) {
        boolean updated = false;
        lock.lock();
        List<String> nodes = cacheData.providerMap.get(svr);
        if (!nodes.equals(providers)) {
            cacheData.providerMap.put(svr, providers);
            updated = true;
        }
        lock.unlock();
        if (updated) {
            notifyServerListChange(svr);
        }
    }
}
