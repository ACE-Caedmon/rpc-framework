package com.xiaoluo.rpc.registry.client;

import com.xiaoluo.rpc.cluster.client.RpcClientTemplate;
import com.xiaoluo.rpc.cluster.client.ServerNode;
import com.xiaoluo.rpc.cluster.client.SimpleRpcClientApi;
import com.xiaoluo.rpc.cluster.server.SimpleRpcServerApi;
import com.xiaoluo.rpc.dispatch.RpcCallInfo;
import com.xiaoluo.rpc.registry.RegistryConstant;
import com.xiaoluo.rpc.registry.RegistryNodeInformation;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.registry.event.RegistryEvent;
import com.xiaoluo.rpc.dispatch.SessionFire;
import com.xiaoluo.utils.NetworkUtil;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Caedmon on 2015/9/24.
 */
public class SimpleRegistryApi {
    private String monitorAddress;
    private static RpcClientTemplate template;
    private ServerNode serverNode;
    private String monitorHost;
    private int monitorPort;
    private String selfHost;
    private String[] groups;
    private int selfPort;
    private Map<String,String> configMap=new HashMap<>();
    private static SimpleRegistryApi instance=new SimpleRegistryApi();
    private static final Logger log= LoggerFactory.getLogger(SimpleRegistryApi.class);
    private volatile boolean connected;
    private volatile boolean registed;
    private RpcCallInfo rpcCallInfo=new RpcCallInfo();
    private List<RegistryEventWatcher> watchers =new ArrayList<>();
    private static final long RECONNECT_PERIOD=10000;
    public static final String MONITOR_SERVER_ADDRESS ="rpc.monitor.address";
    public static final String MONITOR_AUTO_LOCAL_HOST ="rpc.registry.auto.local.host";
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t=new Thread(r);
            t.setName("Rpc-Monitor-Scheduled-Thread");
            return t;
        }
    });
    private Future heatBeatFuture;
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
    private SimpleRegistryApi(){

    }
    public boolean isCompleted(){
        return connected&&registed;
    }
    public static SimpleRegistryApi getInstance(){
        return instance;
    }

    public void addEventWatcher(RegistryEventWatcher watcher){
        watchers.add(watcher);
    }
    public List<RegistryEventWatcher> getWatchers(){
        return watchers;
    }
    public void dispatchEvent(RegistryEvent event){
        log.debug("Rpc registry client dispatch event:type={},value={}", event.getType(), event);
        for(RegistryEventWatcher watcher:watchers){
            watcher.process(event);
        }
    }

    private void load(Properties properties){
        this.monitorAddress=properties.getProperty(MONITOR_SERVER_ADDRESS);
        if(monitorAddress==null){
            throw new NullPointerException("Monitor server address not specified");
        }
        this.groups=properties.getProperty(SimpleRpcServerApi.RPC_SERVER_CLUSTER_NAMES).split(",");
        String autoHost=properties.getProperty(MONITOR_AUTO_LOCAL_HOST);
        if(null!=autoHost&&Boolean.valueOf(autoHost)){
            try{
                this.selfHost= NetworkUtil.getLocalHost();
            }catch (SocketException e){
                e.printStackTrace();
                this.selfHost=null;
            }
        }
        this.selfPort=Integer.parseInt(properties.getProperty(SimpleRpcServerApi.RPC_SERVER_PORT_PROPERTY));
    }
    public void bind(Properties properties){
        load(properties);
        try{
            connect(monitorAddress);
            register();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public synchronized void connect(String monitorAddress) throws Exception{
        if(!connected){
            this.monitorAddress =monitorAddress;
            String[] arr=monitorAddress.split(":");
            this.monitorHost =arr[0];
            this.monitorPort =Integer.parseInt(arr[1]);
            template.setScanPackage(new String[]{"com.xiaoluo.rpc.registry.client"});
            SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, new ClientSessionEventHandler());
            if(serverNode==null){
                this.serverNode=ServerNode.build(RegistryConstant.MONITOR_SERVER_NAME, monitorHost, this.monitorPort,template,null);
            }else{
                this.serverNode.connect();
            }
            this.serverNode.getSession().setAttribute(RegistryNode.RPC_MONITOR_CLIENT, true);
            this.connected=true;

        }

    }
    public synchronized void reconnect() throws Exception{
        log.info("Rpc registry client connect to registry server {}",this.monitorAddress);
        connect(this.monitorAddress);
        if(this.groups!=null){
            register();
        }
    }
    public String getMonitorHost(){
        return monitorHost;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    
    public Map<String, RegistryNode> getAllNodeMap() throws Exception{
        return serverNode.syncCall(RegistryConstant.MonitorServerMethod.GET_ALL_NODE_MAP,Map.class);
    }

    public void register() throws Exception {
        RegistryNode registerNode=null;
        //
        if(this.selfHost!=null){
            registerNode=serverNode.syncCall(RegistryConstant.MonitorServerMethod.REGISTER_WITH_HOST,
                    RegistryNode.class,new Object[]{
                    groups,this.selfHost,selfPort
            });
        }else{
            registerNode=serverNode.syncCall(RegistryConstant.MonitorServerMethod.REGISTER,RegistryNode.class,new Object[]{groups,selfPort});
            this.selfHost=registerNode.getHost();
        }


        SimpleRpcClientApi.getInstance().setRouteTable(registerNode.getRouteTable());
        this.registed=true;
        //注册成功才能开始心跳
        if(this.heatBeatFuture==null||this.heatBeatFuture.isCancelled()){
            this.heatBeatFuture=this.executorService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (!connected || !SimpleRegistryApi.this.serverNode.isActive()) {
                        do {
                            try {
                                SimpleRegistryApi.this.reconnect();
                            } catch (Exception e) {
                                if (e instanceof ConnectException) {
                                    log.error("Can not connect to registry server {},may be registry server is shutdown", SimpleRegistryApi.getInstance().getMonitorAddress());
                                } else {
                                    e.printStackTrace();
                                }
                            } finally {
                                try {
                                    Thread.sleep(RECONNECT_PERIOD);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.error("Thread Sleep error", e);
                                }
                            }
                        } while (!SimpleRegistryApi.getInstance().isConnected());
                    }
                    try {
                        if (SimpleRegistryApi.this.connected) {
                            RegistryNodeInformation information = new RegistryNodeInformation();
                            information.setRpcCallInfo(SimpleRegistryApi.this.rpcCallInfo);
                            SimpleRegistryApi.this.heartBeat(information);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("Rpc registry client heart beat task error", e);
                    }

                }
            }, 1, 10, TimeUnit.SECONDS);
            log.info("Rpc registry start schedule heat beat thread");
        }
    }

    
    public void delete(String key) throws Exception{
        serverNode.syncCall(RegistryConstant.MonitorServerMethod.DELETE,Void.class,key);
    }

    
    public void updateConfig(String key, String config) throws Exception{
        serverNode.syncCall(RegistryConstant.MonitorServerMethod.UPDATE_CONFIG,Void.class,key,config);
    }

    
    public String getConfig(String key) throws Exception{
        return serverNode.syncCall(RegistryConstant.MonitorServerMethod.GET_CONFIG,String.class,key);
    }

    
    public String heartBeat(RegistryNodeInformation information) throws Exception {
        return serverNode.syncCall(RegistryConstant.MonitorServerMethod.HEART_BEAT,String.class,information);
    }

    
    public String bindConfig(String nodeKey, String configKey) throws Exception {
        return serverNode.syncCall(RegistryConstant.MonitorServerMethod.BIND_CONFIG,String.class,nodeKey,configKey);
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
    public void addRpcCallRecord(String cmd,long callTime,long cost){
        rpcCallInfo.addRecord(cmd,callTime,cost);
    }

    public String getMonitorAddress() {
        return monitorAddress;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public String getSelfHost() {
        return selfHost;
    }
}
