package com.xl.rpc.monitor.server;

import com.xl.rpc.monitor.MonitorInformation;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.rpc.monitor.event.ConfigEvent;
import com.xl.rpc.monitor.event.NodeActiveEvent;
import com.xl.rpc.monitor.event.NodeInActiveEvent;
import com.xl.session.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Created by Caedmon on 2015/9/22.
 */
public class RpcMonitorServerService implements IRpcMonitorServerService {
    private static final Logger log= LoggerFactory.getLogger(RpcMonitorServerService.class);
    private static MonitorManager monitorManager = MonitorManager.getInstance();
    @Override
    public Map<String, MonitorNode> getAllNodeMap() {
        return monitorManager.getAllNodeMap();
    }

    @Override
    public String getConfig(String key) {
        return monitorManager.getConfig(key);
    }

    @Override
    public void register(ISession session, String[] groups,int port) throws Exception{
        monitorManager.register(session, groups, session.getClientHost(), port);
    }

    @Override
    public void deleteNode(String key) {
        monitorManager.delete(key);
    }

    @Override
    public void updateConfig(String configKey, String configValue) {
        monitorManager.updateConfig(configKey,configValue);

    }

    @Override
    public String heartBeat(ISession session,MonitorInformation information) {
        String nodeKey=session.getAttribute(MonitorNode.RPC_NODE_KEY);
        if(nodeKey==null){
            throw new IllegalStateException("Client must register first");
        }
        MonitorNode node= monitorManager.getRpcNode(nodeKey);
        node.setActive(true);
        node.setLastActiveTime(System.currentTimeMillis());
        node.setMonitorInformation(information);
        return null;
    }

    @Override
    public String bindNodeConfig(String nodeKey, String configKey) {
        //monitorManager.updateConfigBind(nodeKey, configKey);
        log.info("Bind config:address={},configKey={}", nodeKey, configKey);
        return monitorManager.getConfig(configKey);
    }
}
