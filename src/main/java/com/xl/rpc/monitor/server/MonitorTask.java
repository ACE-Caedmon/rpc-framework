package com.xl.rpc.monitor.server;

import com.xl.rpc.monitor.MonitorNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by Caedmon on 2015/9/24.
 */
public class MonitorTask implements Runnable{
    public static final long TIME_OUT_PERIOD=60;
    private static final Logger log= LoggerFactory.getLogger(MonitorTask.class);
    public MonitorTask(){
    }
    @Override
    public void run() {
        MonitorManager manager=MonitorManager.getInstance();
        Collection<MonitorNode> list=manager.getAllNodeList();
        for(MonitorNode node:list){
            if(node.isActive()){
                log.debug("Idle check:{}",node.getKey());
                long lastActiveTime=node.getLastActiveTime();
                long now=System.currentTimeMillis();
                if(now-lastActiveTime>TIME_OUT_PERIOD*1000){
                    //断开连接
                    manager.disconnectNode(node.getKey());
                    //标记不可用状态
                    node.setActive(false);
                    log.debug("Monitor idle timeout:{}",node.getKey());
                }

            }
        }
    }
}
