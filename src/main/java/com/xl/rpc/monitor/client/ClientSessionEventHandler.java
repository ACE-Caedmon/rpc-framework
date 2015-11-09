package com.xl.rpc.monitor.client;

import com.xl.rpc.event.IEventHandler;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/9/28.
 */
public class ClientSessionEventHandler implements IEventHandler<ISession>{

    private static final Logger log= LoggerFactory.getLogger(ClientSessionEventHandler.class);
    @Override
    public void handleEvent(ISession session) {
        if(session.containsAttribute(MonitorNode.RPC_MONITOR_CLIENT)){
            SimpleRpcMonitorApi.getInstance().setConnected(false);
            log.error("Disconnected from the monitor server");
        }
    }
}
