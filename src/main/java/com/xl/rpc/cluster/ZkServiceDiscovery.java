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
    private List<String> monitorList;
    private static String ServerStore = "/server";
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
                update();
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
    * monitorServerGroups: 监听其他服务器的实例状态
    */
    public void monitorServiceProviders(List<String> monitorServerList) throws Exception{
        this.monitorList = monitorServerList;
        initServiceDiscoveryWatchers();
        update();
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
        createPath(getSvrTreePath());
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
        String path = getSvrTreePath() + "/" + host+":"+port;
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


    private void initServiceDiscoveryWatchers() throws KeeperException,InterruptedException{
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
