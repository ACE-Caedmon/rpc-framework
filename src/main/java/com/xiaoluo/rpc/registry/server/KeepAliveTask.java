package com.xiaoluo.rpc.registry.server;

import com.xiaoluo.rpc.registry.RegistryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by Caedmon on 2015/9/24.
 */
public class KeepAliveTask implements Runnable{
    public static final long TIME_OUT_PERIOD=60;
    private static final Logger log= LoggerFactory.getLogger(KeepAliveTask.class);
    public KeepAliveTask(){
    }
    @Override
    public void run() {
        RegistryManager manager= RegistryManager.getInstance();
        Collection<RegistryNode> list=manager.getAllNodeList();
        for(RegistryNode node:list){
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
