package com.xl.rpc.cluster;

import com.xl.rpc.exception.ClusterException;
import com.xl.utils.Util;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zwc on 2015/7/10.
 */
public class ZkServiceDiscovery {

    private ZooKeeper zkc;
    ServerDiscoveryListener listener;

    private String clusterName;

    private List<String> monitorList = new ArrayList<>();

    private static String ServerStore = "/server";


    private PathWatcher rootPathWather = new PathWatcher(ServerStore);

    private Map<String,PathWatcher> providerWatchers = new HashMap<String, PathWatcher>();

    private static final Logger log= LoggerFactory.getLogger(ZkServiceDiscovery.class);

    private static int Session_Timeout = 5*1000;


    private static String zkServerAddr;
    List<InetSocketAddress> zkAddressList = new ArrayList<>();


    static class CacheData {
        public Map<String,List<String>> providerMap = new HashMap<String, List<String>>(); // 需要lock保护
    }

    private Lock lock = new ReentrantLock();

    private CacheData cacheData = new CacheData();

    public interface ServerDiscoveryListener {
        void onServerListChanged(String server);
    }

    Watcher watcher = new Watcher(){
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                updateAll();
            }
        }
    };

    public ZkServiceDiscovery(String zkServer) {
        zkServer = zkServer.trim();
        zkServerAddr = zkServer;
        parseAddr();
        zkc = ZKClient.getZookeeper(zkServer);
        ZKClient.registerConnectedWatcher(watcher);
    }

    private void parseAddr() {
        int DEFAULT_PORT = 2181;
        String hostsList[] = zkServerAddr.split(",");
        for (String host : hostsList) {
            int port = DEFAULT_PORT;
            int pidx = host.lastIndexOf(':');
            if (pidx >= 0) {
                if (pidx < host.length() - 1) {
                    port = Integer.parseInt(host.substring(pidx + 1));
                }
                host = host.substring(0, pidx);
            }
            zkAddressList.add(new InetSocketAddress(host,port));
        }
    }


    /*
     * if svrName not in the monitorList,add it
     */
    public List<String> getServerList(String svrName) {
        return cacheData.providerMap.get(svrName);
    }

    public Map<String,List<String>> getAllServerMap(){
        updateAll();
        return cacheData.providerMap;
    }


    /*
    * logicName:logicName
    * address:ip:port
    */
    public void registerService(String logicName,String host,int port) throws Exception{
        log.debug("registerService logicName {} host {} port {}", logicName,host,port);
        if (Util.isEmpty(logicName)) {
            throw new RuntimeException("ClusterName or server address is null");
        }
        this.clusterName = logicName;
        createPath(ServerStore + "/" + clusterName);
        if (Util.isEmpty(host)) {
            String localIp = null;
            for (InetSocketAddress svr : zkAddressList) {
                try {
                    Socket socket = new Socket();
                    socket.connect(svr,5000);
                    localIp = socket.getLocalAddress().getHostAddress();
                    log.debug("determined local ip {}", localIp);
                    socket.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    localIp = null;
                }
            }
            host = localIp;
            if (Util.isEmpty(host)) {
                throw new RuntimeException("can not determine the local ip");
            }
        }
        String path = ServerStore + "/" + clusterName + "/" + host+":"+port;
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


    public void setListener(ServerDiscoveryListener listener) {
        this.listener = listener;
    }

    private synchronized void updateAll() {
        try {
            List<String> services = getServerListByPath(ServerStore);
            for (String e:services) {
                if (!monitorList.contains(e)) {
                    monitorList.add(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        update();
        log.info("servers {}",cacheData.providerMap);
    }


    void update() throws ClusterException{
        if (zkc == null)
            return;
        try{
            rootPathWather.monitor();

            createPath(ServerStore);

            for (String svrName : monitorList) {
                List<String> nodes = getServerListByPath(ServerStore + "/" + svrName);
                saveProviders(svrName,nodes);
            }

            for (String svrName :monitorList) {
                String path = ServerStore+"/"+svrName;
                if (!providerWatchers.containsKey(path)) {
                    PathWatcher watcher = new PathWatcher(path);
                    providerWatchers.put(path,watcher);
                }
            }

            for (String svrName : monitorList) {
                String path = ServerStore+"/"+svrName;
                PathWatcher pathWatcher = providerWatchers.get(path);
                if(pathWatcher!=null){
                    pathWatcher.monitor();
                }
            }

        }catch (Exception e){
            throw new ClusterException("Update zookeeper nodes error",e);
        }

    }


    public void close() throws InterruptedException{
        zkc.close();
    }


    private List<String> getServerListByPath(String path) {
        List<String> children = null;
        try {
            children = zkc.getChildren(path,false);
        } catch (Exception e) {
            e.printStackTrace();
            children = null;
        }
        return children;
    }


    private void createPath(String path){
        try {
            Stat stat = zkc.exists(path, false);
            if (stat == null) {
                zkc.create(path, "".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {

        }
    }


    // path monitor
    class PathWatcher implements Watcher {
        String path;
        public PathWatcher(String path) {
            this.path = path;
        }
        public void monitor() {
            createPath(path);
            try {
                zkc.getChildren(path, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.NodeCreated ||
                    watchedEvent.getType() == Event.EventType.NodeDataChanged ||
                    watchedEvent.getType() == Event.EventType.NodeDeleted ||
                    watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                String path = watchedEvent.getPath();
                if (ServerStore.equals(path)) {
                    updateAll();
                } else {
                    try{
                        int index = path.lastIndexOf("/");
                        String service = path.substring(index + 1);
                        List<String> providers = getServerListByPath(path);
                        saveProviders(service, providers);
                    }catch (Exception e){
                        e.printStackTrace();
                        log.error("Zookeeper process WatchEvent error:server = {}", path, e);
                    }
                }
                monitor();
            }
        }
    }



    private void saveProviders(String svr,List<String> providers) {
        if (providers == null) {
            return;
        }
        boolean updated;
        lock.lock();
        List<String> nodes = cacheData.providerMap.get(svr);
        if (providers.equals(nodes)) {
            return;
        }
        cacheData.providerMap.put(svr, providers);
        updated = true;
        lock.unlock();
        if (updated) {
            if (listener != null) {
                listener.onServerListChanged(svr);
            }
        }
    }
}
