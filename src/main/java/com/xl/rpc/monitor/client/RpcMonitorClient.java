package com.xl.rpc.monitor.client;

import com.xl.rpc.cluster.client.RpcClientTemplate;
import com.xl.rpc.cluster.client.ServerNode;
import com.xl.rpc.monitor.MonitorConstant;
import com.xl.rpc.monitor.MonitorInformation;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.SessionFire;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * Created by Administrator on 2015/9/24.
 */
public class RpcMonitorClient {
    private String centerAddress;
    private static RpcClientTemplate template;
    private ServerNode serverNode;
    private String host;
    private int port;
    private String[] groups;
    private Map<String,String> configMap=new HashMap<>();
    private static RpcMonitorClient instance=new RpcMonitorClient();
    private volatile boolean connected;
    static {
        template=RpcClientTemplate.newDefault();
        template.setCmdThreadSize(1);
        template.setLoopGroup(new NioEventLoopGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t=new Thread(r);
                t.setName("Rpc-Monitor-Client-Thread");
                return t;
            }
        }));
    }
    private RpcMonitorClient(){

    }
    public static RpcMonitorClient getInstance(){
        return instance;
    }
    public synchronized void connect(String centerAddress) throws Exception{
        if(!connected){
            this.centerAddress=centerAddress;
            String[] arr=centerAddress.split(":");
            this.host=arr[0];
            this.port=Integer.parseInt(arr[1]);
            template.setScanPackage(new String[]{"com.xl"});
            SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, new ClientSessionEventHandler());
            this.serverNode=ServerNode.build(MonitorConstant.MONITOR_SERVER_NAME,host,port,template,null);
            this.serverNode.getSession().setAttribute(MonitorNode.RPC_MONITOR_CLIENT,true);
            this.connected=true;
        }

    }
    public synchronized void reconnect() throws Exception{
        connect(this.centerAddress);
        if(this.groups!=null){
            register(this.groups,this.port);
        }
    }
    public String getHost(){
        return host;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    
    public Map<String, MonitorNode> getAllNodeMap() throws Exception{
        return serverNode.syncCall(MonitorConstant.MonitorServerMethod.GET_ALL_NODE_MAP,Map.class);
    }

    
    public void register(String[] groups, int port) throws Exception {
        this.groups=groups;
        this.port=port;
        serverNode.syncCall(MonitorConstant.MonitorServerMethod.REGISTER,Void.class,new Object[]{groups,port});
    }

    
    public void delete(String key) throws Exception{
        serverNode.syncCall(MonitorConstant.MonitorServerMethod.DELETE,Void.class,key);
    }

    
    public void updateConfig(String key, String config) throws Exception{
        serverNode.syncCall(MonitorConstant.MonitorServerMethod.UPDATE_CONFIG,Void.class,key,config);
    }

    
    public String getConfig(String key) throws Exception{
        return serverNode.syncCall(MonitorConstant.MonitorServerMethod.GET_CONFIG,String.class,key);
    }

    
    public String heartBeat(MonitorInformation information) throws Exception {
        return serverNode.syncCall(MonitorConstant.MonitorServerMethod.HEART_BEAT,String.class,information);
    }

    
    public String bindConfig(String nodeKey, String configKey) throws Exception {
        return serverNode.syncCall(MonitorConstant.MonitorServerMethod.BIND_CONFIG,String.class,nodeKey,configKey);
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
