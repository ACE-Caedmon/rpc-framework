package com.xl.rpc.monitor.client;

import com.xl.rpc.event.IEventHandler;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.ISession;

/**
 * Created by Administrator on 2015/9/28.
 */
public class ClientSessionEventHandler implements IEventHandler<ISession>{
    public static final long RECONNECT_PERIOD=10000;
    @Override
    public void handleEvent(ISession session) {
        if(session.containsAttribute(MonitorNode.RPC_MONITOR_CLIENT)){
            RpcMonitorClient.getInstance().setConnected(false);
            do{
                try {
                    RpcMonitorClient.getInstance().reconnect();
                    Thread.sleep(RECONNECT_PERIOD);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }while (!RpcMonitorClient.getInstance().isConnected());

        }
    }
}
