package com.xl.rpc.monitor.server;

import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import com.xl.rpc.internal.InternalContainer;
import com.xl.session.SessionFire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by Administrator on 2015/9/28.
 */
public class MonitorServer {
    private int port;
    private static final Logger log= LoggerFactory.getLogger(MonitorServer.class);
    public MonitorServer(int port){
        this.port=port;
    }
    public void start(){
        InternalContainer container=InternalContainer.getInstance();
        Properties properties=new Properties();
        properties.setProperty(SimpleRpcServerApi.RPC_SERVER_PORT_PROPERTY,String.valueOf(this.port));
        properties.setProperty(SimpleRpcServerApi.SCAN_PACKAGE_NAME_PROPERTY,"com.xl.rpc.monitor.server");
        properties.setProperty(SimpleRpcServerApi.RPC_SERVER_CLUSTER_NAMES,"monitor");
        properties.setProperty(SimpleRpcServerApi.BEAN_ACCESS_PROPERTY,"com.xl.rpc.internal.SpringBeanAccess");
        SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, new ServerSessionEventHandler());
        container.startRpcServer(properties);
        MonitorManager.getInstance().start();
        log.info("Monitor Server Start Complete,bind port "+port);
    }
}
