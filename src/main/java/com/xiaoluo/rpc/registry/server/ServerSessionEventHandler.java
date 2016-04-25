package com.xiaoluo.rpc.registry.server;

import com.xiaoluo.rpc.event.IEventHandler;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.dispatch.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/9/28.
 */
public class ServerSessionEventHandler implements IEventHandler<ISession>{
    private static final Logger log= LoggerFactory.getLogger(ServerSessionEventHandler.class);
    @Override
    public void handleEvent(ISession session) {
        if (session.containsAttribute(RegistryNode.RPC_NODE_KEY)) {
            String nodeKey = session.getAttribute(RegistryNode.RPC_NODE_KEY);
            if (nodeKey != null) {
                RegistryManager.getInstance().disconnectNode(nodeKey);
                log.info("Rpc node disconnect:key={}",nodeKey);
            }
        }
    }
}
