package com.xl.rpc.cluster;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by issac on 15/8/10.
 */
public class ZKClient {
    private static final Logger log = LoggerFactory.getLogger(ZKClient.class);
    private ZooKeeper zkc;
    public static final int Session_Timeout = 8*1000;
    private Lock lock = new ReentrantLock();
    private List<Watcher> connectedWacther = new ArrayList<>();
    private static final ZKClient instance=new ZKClient();
    private static Set<KeeperException.Code> needNewKeeperCodeSet=new HashSet<>();

    static {
//        needNewKeeperCodeSet.add(KeeperException.Code.CONNECTIONLOSS);
//        needNewKeeperCodeSet.add(KeeperException.Code.OPERATIONTIMEOUT);
        needNewKeeperCodeSet.add(KeeperException.Code.SESSIONEXPIRED);
//        needNewKeeperCodeSet.add(KeeperException.Code.INVALIDACL);
//        needNewKeeperCodeSet.add(KeeperException.Code.AUTHFAILED);
//        needNewKeeperCodeSet.add(KeeperException.Code.SESSIONMOVED);
    }
    private ZKClient(){

    }
    public static ZKClient getInstance(){
        return instance;
    }
    Watcher watcher = new Watcher(){
        // 监控所有被触发的事件
        public void process(WatchedEvent event) {
            log.info("process {}",event);
            if (event.getState() == Event.KeeperState.SyncConnected) {
                lock.lock();
                List<Watcher> clone = new ArrayList<>(connectedWacther);
                lock.unlock();
                for (Watcher watcher:clone) {
                    watcher.process(event);
                }
            }
        }
    };


    public void registerConnectedWatcher(Watcher watcher) {
        if (!connectedWacther.contains(watcher)) {
            lock.lock();
            connectedWacther.add(watcher);
            lock.unlock();
            if (zkc != null && zkc.getState() == ZooKeeper.States.CONNECTED) {
                watcher.process(new WatchedEvent(Watcher.Event.EventType.None, Watcher.Event.KeeperState.SyncConnected,null));
            }
        }
    }

    public ZooKeeper getZookeeper(String zookeeperAddress) {
        try{
            if (zkc == null) {
                zkc=createZookeeper(zookeeperAddress);
            }
            else{
                try{
                    Stat stat=zkc.exists("/server", false);
                    if(stat==null){
                        zkc.create("/server","".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
                    }
                }catch (KeeperException e){
                    e.printStackTrace();
                    KeeperException.Code code=e.code();
                    if(needNewKeeperCodeSet.contains(code)){
                        zkc.close();
                        log.warn("Zookeeper need to create new one:code={},address={}",code,zookeeperAddress);
                        zkc=createZookeeper(zookeeperAddress);
                    }else{
                        throw e;
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Get zookeeper error:" + zookeeperAddress, e);
            zkc=null;
        }

        return zkc;
    }
    public ZooKeeper createZookeeper(String address) throws Exception{
        ZooKeeper zooKeeper=new ZooKeeper(address,Session_Timeout,watcher);
        log.info("Create new zookeeper:{}",address);
        return zooKeeper;
    }

    public static void main(String[] args) throws Exception{
        ZooKeeper zooKeeper=new ZooKeeper("192.168.1.168:2181", Session_Timeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {

            }
        });
        Id id = new Id("world", "anyone"); //放开权限

        ACL acl = new ACL(ZooDefs.Perms.ALL, id);
        List<ACL> list=new ArrayList<>();
        list.add(acl);
        zooKeeper.create("/server/msg/test", "".getBytes(), list, CreateMode.EPHEMERAL);
    }
}
