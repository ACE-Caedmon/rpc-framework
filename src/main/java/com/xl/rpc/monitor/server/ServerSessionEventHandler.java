package com.xl.rpc.monitor.server;

import com.xl.rpc.event.IEventHandler;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/9/28.
 */
public class ServerSessionEventHandler implements IEventHandler<ISession>{
    private static final Logger log= LoggerFactory.getLogger(ServerSessionEventHandler.class);
    @Override
    public void handleEvent(ISession session) {
        if (session.containsAttribute(MonitorNode.RPC_NODE_KEY)) {
            String nodeKey = session.getAttribute(MonitorNode.RPC_NODE_KEY);
            if (nodeKey != null) {
                MonitorManager.getInstance().disconnectNode(nodeKey);
                log.info("Rpc node disconnect:key={}",nodeKey);
            }
        }
    }
}
