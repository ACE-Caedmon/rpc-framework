package com.xl.rpc.cluster;

import com.xl.rpc.cluster.zookeeper.ZkPathWatcher;
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
    private static final Logger log= LoggerFactory.getLogger(ZkServiceDiscovery.class);
    private ZooKeeper zkc;
    ServerDiscoveryListener listener;
    private String clusterName;
    private List<String> monitorList = new ArrayList<>();
    public static final String ServerStore = "/server";
    private ZkPathWatcher rootPathWather = new ZkPathWatcher(this,ServerStore);

    private Map<String,ZkPathWatcher> providerWatchers = new HashMap<String, ZkPathWatcher>();

    List<InetSocketAddress> zookeeperAddressList = new ArrayList<>();


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
            log.info("ZkServiceDiscovery process {}",event);
            if (event.getState() == Event.KeeperState.SyncConnected) {
                updateAll();
            }
        }
    };

    public ZkServiceDiscovery(String zookeeperAddress) {
        int DEFAULT_PORT = 2181;
        String hostsList[] = zookeeperAddress.split(",");
        for (String host : hostsList) {
            int port = DEFAULT_PORT;
            int pidx = host.lastIndexOf(':');
            if (pidx >= 0) {
                if (pidx < host.length() - 1) {
                    port = Integer.parseInt(host.substring(pidx + 1));
                }
                host = host.substring(0, pidx);
            }
            zookeeperAddressList.add(new InetSocketAddress(host, port));
        }
        zkc = ZKClient.getInstance().getZookeeper(zookeeperAddress);
        ZKClient.getInstance().registerConnectedWatcher(watcher);
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
        if (Util.isEmpty(logicName)) {
            throw new RuntimeException("ClusterName or server address is null");
        }
        this.clusterName = logicName;
        createPath(ServerStore + "/" + clusterName);
        if (Util.isEmpty(host)) {
            String localIp = null;
            for (InetSocketAddress svr : zookeeperAddressList) {
                try {
                    Socket socket = new Socket();
                    socket.connect(svr,5000);
                    localIp = socket.getLocalAddress().getHostAddress();
                    log.debug("Determined local ip {}", localIp);
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
            Thread.sleep((long) (ZKClient.Session_Timeout * 1.5));
        }
        zkc.create(path, clusterName.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        log.debug("Register cluster success:clusterName = {}",logicName);
        update();
    }


    public void setListener(ServerDiscoveryListener listener) {
        this.listener = listener;
    }

    public synchronized void updateAll() {
        try {
            createPath(ServerStore);
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
        log.info("Update servers {}",cacheData.providerMap);
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
                    ZkPathWatcher watcher = new ZkPathWatcher(this,path);
                    providerWatchers.put(path,watcher);
                }
            }

            for (String svrName : monitorList) {
                String path = ServerStore+"/"+svrName;
                ZkPathWatcher pathWatcher = providerWatchers.get(path);
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


    public List<String> getServerListByPath(String path) {
        List<String> children = null;
        try {
            children = zkc.getChildren(path,false);
        } catch (Exception e) {
            e.printStackTrace();
            children = null;
        }
        return children;
    }


    public void createPath(String path){
        try {
            Stat stat = zkc.exists(path, false);
            if (stat == null) {
                zkc.create(path, "".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void saveProviders(String svr,List<String> providers) {
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
    public ZooKeeper getZookeeper(){
        return zkc;
    }
}
