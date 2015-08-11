package com.xl.rpc.cluster;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by issac on 15/8/10.
 */
public class ZKClient {
    private static final Logger log = LoggerFactory.getLogger(ZKClient.class);
    private ZooKeeper zkc;
    public static final int Session_Timeout = 10*1000;
    private Lock lock = new ReentrantLock();
    private List<Watcher> connectedWacther = new ArrayList<>();
    private static final ZKClient instance=new ZKClient();

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
        if (zkc == null) {
            try {
                zkc = new ZooKeeper(zookeeperAddress, Session_Timeout, watcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!zookeeperAddress.equals(zookeeperAddress)) {
                throw  new RuntimeException("zookeeper server addr must unique");
            }
        }
        return zkc;
    }
}
