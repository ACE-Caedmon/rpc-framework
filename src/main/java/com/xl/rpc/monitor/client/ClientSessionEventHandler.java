package com.xl.rpc.monitor.client;

import com.xl.rpc.event.IEventHandler;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.ISession;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2015/9/28.
 */
public class ClientSessionEventHandler implements IEventHandler<ISession>{
    public static final long RECONNECT_PERIOD=10000;
    private static final ScheduledExecutorService RECONNECT_THREAD_POOL= Executors.newSingleThreadScheduledExecutor();
    @Override
    public void handleEvent(ISession session) {
        if(session.containsAttribute(MonitorNode.RPC_MONITOR_CLIENT)){
            RpcMonitorClient.getInstance().setConnected(false);
            RECONNECT_THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {
                    do{
                        try {
                            RpcMonitorClient.getInstance().reconnect();
                            Thread.sleep(RECONNECT_PERIOD);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }while (!RpcMonitorClient.getInstance().isConnected());
                }
            });


        }
    }
}
