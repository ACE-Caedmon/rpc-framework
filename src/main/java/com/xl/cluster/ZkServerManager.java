package com.xl.cluster;

import com.alibaba.fastjson.JSONObject;
import com.xl.utils.Util;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    private String svrName;
    private List<String> monitorList;
    private String cachePath;

    private static String ServerStore = "/server";
    private static String ConfigStore = "/config";
    private static String CacheDataFile = "cache.txt";

    private DataWatcher configWatcher;
    private String address;

    private Map<String,PathWatcher> providerWatchers = new HashMap<String, PathWatcher>();
    private static final Logger log= LoggerFactory.getLogger(ZkServerManager.class);
    private static int Session_Timeout = 5*1000;
    private static String ZK_SERVER = "192.168.1.168:2181";

    static class CacheData {
        public String config;
        public Map<String,List<String>> providerMap = new HashMap<String, List<String>>(); // 需要lock保护
    }

    private Lock lock = new ReentrantLock();

    private CacheData cacheData = new CacheData();

    public interface ServerConfigListener {
        // 该服务器的配置发生变化
        void onConfigChanged(String config);
        void onServerListChanged(String server);
    }

    Watcher watcher = new Watcher(){
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                try {
                    update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public ZkServerManager(String cachePath) {
        this.cachePath = cachePath;
        loadFromFile();
        try {
            zkc = new ZooKeeper(ZK_SERVER, Session_Timeout, watcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /*
    * monitorServerGroups: 监听其他服务器的实例状态
    */
    public void monitorServiceProviders(List<String> monitorServerList) {
        this.monitorList = monitorServerList;
        initNameServiceWatchers();
    }

    /*
    * logicName:logicName
    * address:ip:port
    */
    public void registerService(String logicName,String address) {
        if (Util.isEmpty(address) || Util.isEmpty(logicName)) {
            throw new RuntimeException("param null");
        }
        this.address = address;
        this.svrName = logicName;
        configWatcher = new DataWatcher(getConfigPath());
        try {
            createPath(getSvrTreePath());
            String path = getSvrTreePath() + "/" + address;
            Stat stat = zkc.exists(path,false);
            // 说明程序挂了，立马又被拉起，这时候需要等zk服务器超时了再注册
            if (stat != null)
                Thread.sleep((long) (Session_Timeout*1.5));
            zkc.create(path, svrName.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setListener(ServerConfigListener listener) {
        this.listener = listener;
    }


    private void loadFromFile() {
        File f=new File(this.cachePath,CacheDataFile);
        if(!f.exists()){
            try{
                f.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        String path = f.getAbsolutePath();
        String data = Util.getFileData(path);
        log.debug("load zookeeper cache: data = {},path = {} ", data, path);
        if (!Util.isEmpty(data)) {
            cacheData= JSONObject.parseObject(data,CacheData.class);
        } else {
            cacheData = new CacheData();
        }
    }

    private void saveCacheConfigToFile() {
        String path = new File(this.cachePath,CacheDataFile).getAbsolutePath();
        String data =JSONObject.toJSONString(cacheData);
        log.info("save config: data = {},path = {} ",data,path);
        Util.saveFileData(path, data);
    }





    private void initNameServiceWatchers() {
        if (monitorList != null) {
            for (String svrName : monitorList) {
                String path = ServerStore + "/" + svrName;
                if (!providerWatchers.containsKey(svrName)) {
                    PathWatcher pathWatcher = new PathWatcher(path);
                    providerWatchers.put(svrName,pathWatcher);
                }
                try {
                    PathWatcher pathWatcher = providerWatchers.get(svrName);
                    pathWatcher.monitor();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }


    synchronized void update() throws Exception {
        if (zkc == null)
            return;
        createPath(ServerStore);
        createPath(ConfigStore);
        if (!Util.isEmpty(svrName)) {
            String config = getData(getConfigPath());
            log.debug("Zookeeper update: data = {}",config);
            saveConfigData(config);
            configWatcher.monitor();
        }
        // query provider list
        if (monitorList != null) {
            for (String svrName : monitorList) {
                List<String> nodes = null;
                nodes = getServerListByPath(ServerStore+"/"+svrName);
                cacheData.providerMap.put(svrName,nodes);
            }
        }

        //  monitor provider server list
        if (monitorList != null) {
            for (String svrName : monitorList) {
                PathWatcher pathWatcher = providerWatchers.get(svrName);
                pathWatcher.monitor();
            }
        }
    }


    public void close() {
        try {
            zkc.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public String getConfig(){
        return cacheData.config;
    }

    public Map<String,List<String>> getAllServerMap(){
        return cacheData.providerMap;
    }

    public List<String> getServerList(String svrName) {
        if (cacheData.providerMap.containsKey(svrName)) {
            return cacheData.providerMap.get(svrName);
        }
        try {
            update();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cacheData.providerMap.get(svrName);
    }

    private List<String> getServerListByPath(String path) throws KeeperException, InterruptedException {
        List<String> children = zkc.getChildren(path,false);
        return children;
    }

    private String getData(String path) throws Exception {
        byte[] data = zkc.getData(path,false,null);
        return new String(data);
    }


    private String getConfigPath() {
        String path = ConfigStore + "/" + svrName;
        return path;
    }

    private String getSvrTreePath() {
        String path = ServerStore + "/" + svrName;
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
            int index = path.lastIndexOf("/");
            String service = path.substring(index + 1);
            try{
                monitor();
                providers = getServerListByPath(watchedEvent.getPath());
                saveProviders(service, providers);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    // data monitor
    class DataWatcher implements Watcher {
        String path;
        public DataWatcher(String path) {
            this.path = path;
        }
        public void monitor() throws KeeperException, InterruptedException {
            createPath(path);
            zkc.exists(path, this);
        }

        public void process(WatchedEvent watchedEvent) {
            if  (watchedEvent.getType() == Event.EventType.None && watchedEvent.getState() == Event.KeeperState.Disconnected)
                return;
            String data = null;
            try {
                monitor();
                data = getData(watchedEvent.getPath());
                saveConfigData(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyConfigChange() {
        if (listener != null) {
            listener.onConfigChanged(cacheData.config);
        }
    }

    private void notifyServerListChange(String svrName) {
        if (listener != null) {
            listener.onServerListChanged(svrName);
        }
    }

    private void saveConfigData(String config) {
        boolean updated = false;
        lock.lock();
        if (config == null && cacheData.config != null ||
                !config.equals(cacheData.config)) {
            cacheData.config = config;
            updated = true;
        }
        lock.unlock();
        saveCacheConfigToFile();
        if (updated)
            notifyConfigChange();
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
        saveCacheConfigToFile();
        if (updated) {
            notifyServerListChange(svr);
        }
    }
}
