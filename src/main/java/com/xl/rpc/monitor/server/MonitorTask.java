package com.xl.rpc.monitor.server;

import com.xl.rpc.monitor.MonitorNode;

import java.util.Collection;

/**
 * Created by Administrator on 2015/9/24.
 */
public class MonitorTask implements Runnable{
    private MonitorManager manager;
    private static final long TIME_OUT_PERIOD=60;
    public MonitorTask(MonitorManager manager){
        this.manager=manager;
    }
    @Override
    public void run() {
        Collection<MonitorNode> list=manager.getAllNodeList();
        for(MonitorNode node:list){
            long lastActiveTime=node.getLastActiveTime();
            long now=System.currentTimeMillis();
            if(now-lastActiveTime>TIME_OUT_PERIOD*1000){
                //断开连接

                //标记不可用状态
            }
        }
    }
}
