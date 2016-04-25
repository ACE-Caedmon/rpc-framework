package com.xiaoluo.rpc.registry.server;

import com.xiaoluo.rpc.registry.RegistryNodeInformation;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.dispatch.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Caedmon on 2015/9/22.
 */
public class RpcRegistryServerService implements IRpcRegistryServerService {
    private static final Logger log= LoggerFactory.getLogger(RpcRegistryServerService.class);
    private static RegistryManager registryManager = RegistryManager.getInstance();
    @Override
    public Map<String, RegistryNode> getAllNodeMap() {
        return registryManager.getAllNodeMap();
    }

    @Override
    public String getConfig(String key) {
        return registryManager.getConfig(key);
    }

    @Override
    public RegistryNode registerWithHost(ISession session, String[] groups, String host, int port) throws Exception{
        log.info("Register with host {}:{}",host,port);
        return registryManager.register(session, groups, host, port);
    }

    @Override
    public RegistryNode register(ISession session, String[] groups, int port) throws Exception{
        log.info("Register without host {}:{}",session.getClientHost(),port);
        return registryManager.register(session, groups, session.getClientHost(), port);
    }

    @Override
    public void deleteNode(String key) {
        registryManager.delete(key);
    }

    @Override
    public void updateConfig(String configKey, String configValue) {
        registryManager.updateConfig(configKey,configValue);

    }

    @Override
    public String heartBeat(ISession session,RegistryNodeInformation information) {
        String nodeKey=session.getAttribute(RegistryNode.RPC_NODE_KEY);
        if(nodeKey==null){
            throw new IllegalStateException("Client must register first:"+session.getChannel().toString());
        }
        RegistryNode node= registryManager.getRpcNode(nodeKey);
        node.setActive(true);
        node.setLastActiveTime(System.currentTimeMillis());
        node.setRegistryNodeInformation(information);
        return null;
    }

    @Override
    public String bindNodeConfig(String nodeKey, String configKey) {
        //registryManager.updateConfigBind(nodeKey, configKey);
        log.info("Bind config:address={},configKey={}", nodeKey, configKey);
        return registryManager.getConfig(configKey);
    }
}
