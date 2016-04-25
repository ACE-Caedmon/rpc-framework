package com.xiaoluo.rpc.registry.server;

import com.xiaoluo.rpc.cluster.server.SimpleRpcServerApi;
import com.xiaoluo.rpc.internal.InternalContainer;
import com.xiaoluo.rpc.dispatch.SessionFire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by Administrator on 2015/9/28.
 */
public class RegistryServer {
    private int port;
    private static final Logger log= LoggerFactory.getLogger(RegistryServer.class);
    public RegistryServer(int port){
        this.port=port;
    }
    public void start(){
        InternalContainer container=InternalContainer.getInstance();
        Properties properties=new Properties();
        properties.setProperty(SimpleRpcServerApi.RPC_SERVER_PORT_PROPERTY,String.valueOf(this.port));
        properties.setProperty(SimpleRpcServerApi.SCAN_PACKAGE_NAME_PROPERTY,"com.xiaoluo.rpc.registry.server");
        properties.setProperty(SimpleRpcServerApi.RPC_SERVER_CLUSTER_NAMES,"registry");
        properties.setProperty(SimpleRpcServerApi.BEAN_ACCESS_PROPERTY,"com.xiaoluo.rpc.internal.SpringBeanAccess");
        SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, new ServerSessionEventHandler());
        container.startRpcServer(properties);
        RegistryManager.getInstance().start();
        log.info("Monitor Server Start Complete,bind port "+port);
    }
}
