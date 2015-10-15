package com.xl.rpc.monitor.client;

import com.xl.rpc.cluster.client.IClusterServerManager;
import com.xl.rpc.cluster.client.SimpleRpcClientApi;
import com.xl.rpc.monitor.event.ConfigEvent;
import com.xl.rpc.monitor.event.NodeActiveEvent;
import com.xl.rpc.monitor.event.MonitorEvent;
import com.xl.rpc.monitor.event.NodeInActiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/9/17.
 */
public class RpcMonitorClientService implements IRpcMonitorClientService {
    private static final Logger log= LoggerFactory.getLogger(RpcMonitorClientService.class);
    @Override
    public void handleNodeEvent(MonitorEvent event) {
        SimpleRpcClientApi clientApi=SimpleRpcClientApi.getInstance();
        log.info("Handle monitor event:type={},event={}",event.getType(),event);
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

                RpcMonitorClient.getInstance().setConfigMap(configEvent.getConfigMap());
                break;
        }
        RpcMonitorClient.getInstance().dispatchEvent(event);
    }
}
