package com.xiaoluo.rpc.registry.client;

import com.xiaoluo.rpc.cluster.client.IClusterServerManager;
import com.xiaoluo.rpc.cluster.client.SimpleRpcClientApi;
import com.xiaoluo.rpc.registry.event.ConfigEvent;
import com.xiaoluo.rpc.registry.event.NodeActiveEvent;
import com.xiaoluo.rpc.registry.event.RegistryEvent;
import com.xiaoluo.rpc.registry.event.NodeInActiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/9/17.
 */
public class RpcRegistryClientService implements IRpcRegistryClientService {
    private static final Logger log= LoggerFactory.getLogger(RpcRegistryClientService.class);
    @Override
    public void handleNodeEvent(RegistryEvent event) {
        SimpleRpcClientApi clientApi=SimpleRpcClientApi.getInstance();
        log.info("Handle registry event:type={},event={}",event.getType(),event);
        IClusterServerManager serverManager=clientApi.getServerManager();
        if(serverManager==null){
            return;
        }
        switch (event.getType()){
            case NODE_ACTIVE:
                NodeActiveEvent activeEvent=(NodeActiveEvent)event;
                serverManager.addServerNode(activeEvent.getGroup(),activeEvent.getHost(),activeEvent.getPort());
                break;
            case NODE_INACTIVE:
                NodeInActiveEvent inActiveEvent=(NodeInActiveEvent)event;
                serverManager.deleteNode(inActiveEvent.getNodeKey());
                break;
            case CONFIG_UPDATED:
                ConfigEvent configEvent=(ConfigEvent)event;
                SimpleRegistryApi.getInstance().setConfigMap(configEvent.getConfigMap());
                break;
        }
        SimpleRegistryApi.getInstance().dispatchEvent(event);
    }
}
