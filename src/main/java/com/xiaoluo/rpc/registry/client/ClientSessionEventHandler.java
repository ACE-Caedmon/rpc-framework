package com.xiaoluo.rpc.registry.client;

import com.xiaoluo.rpc.event.IEventHandler;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.dispatch.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/9/28.
 */
public class ClientSessionEventHandler implements IEventHandler<ISession>{

    private static final Logger log= LoggerFactory.getLogger(ClientSessionEventHandler.class);
    @Override
    public void handleEvent(ISession session) {
        if(session.containsAttribute(RegistryNode.RPC_MONITOR_CLIENT)){
            SimpleRegistryApi.getInstance().setConnected(false);
            log.error("Disconnected from the registry server");
        }
    }
}
