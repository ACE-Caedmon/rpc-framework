package com.xl.rpc.monitor.client;

import com.xl.rpc.cluster.client.RpcClientTemplate;
import com.xl.rpc.cluster.client.ServerNode;
import com.xl.rpc.dispatch.RpcCallInfo;
import com.xl.rpc.monitor.MonitorConstant;
import com.xl.rpc.monitor.MonitorInformation;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.rpc.monitor.event.MonitorEvent;
import com.xl.session.SessionFire;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Caedmon on 2015/9/24.
 */
public class RpcMonitorClient {
    private String monitorAddress;
    private static RpcClientTemplate template;
    private ServerNode serverNode;
    private String monitorHost;
    private int monitorPort;
    private String[] groups;
    private int selfPort;
    private Map<String,String> configMap=new HashMap<>();
    private static RpcMonitorClient instance=new RpcMonitorClient();
    private static final Logger log= LoggerFactory.getLogger(RpcMonitorClient.class);
    private volatile boolean connected;
    private RpcCallInfo rpcCallInfo=new RpcCallInfo();
    private boolean recordRpcCall =false;
    private List<MonitorEventWatcher> watchers =new ArrayList<>();
    private static final long RECONNECT_PERIOD=10000;
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
    private RpcMonitorClient(){

    }
    public static RpcMonitorClient getInstance(){
        return instance;
    }

    public boolean isRecordRpcCall() {
        return recordRpcCall;
    }

    public void setRecordRpcCall(boolean recordRpcCall) {
        this.recordRpcCall = recordRpcCall;
    }

    public void addEventWatcher(MonitorEventWatcher watcher){
        watchers.add(watcher);
    }
    public List<MonitorEventWatcher> getWatchers(){
        return watchers;
    }
    public void dispatchEvent(MonitorEvent event){
        log.debug("Rpc monitor client dispatch event:type={},value={}", event.getType(), event);
        for(MonitorEventWatcher watcher:watchers){
            watcher.process(event);
        }
    }
    public synchronized void connect(String monitorAddress) throws Exception{
        if(monitorAddress==null){
            throw new NullPointerException("Monitor server address not specified");
        }
        if(!connected){
            this.monitorAddress =monitorAddress;
            String[] arr=monitorAddress.split(":");
            this.monitorHost =arr[0];
            this.monitorPort =Integer.parseInt(arr[1]);
            template.setScanPackage(new String[]{"com.xl.rpc.monitor.client"});
            SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, new ClientSessionEventHandler());
            this.serverNode=ServerNode.build(MonitorConstant.MONITOR_SERVER_NAME, monitorHost, this.monitorPort,template,null);
            this.serverNode.getSession().setAttribute(MonitorNode.RPC_MONITOR_CLIENT,true);
            this.connected=true;
            if(this.heatBeatFuture==null||this.heatBeatFuture.isCancelled()){
                this.heatBeatFuture=this.executorService.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        if (!connected || !RpcMonitorClient.this.serverNode.isActive()) {
                            do {
                                try {
                                    RpcMonitorClient.getInstance().reconnect();
                                } catch (Exception e) {
                                    if (e.getCause() instanceof ConnectException) {
                                        log.error("Can not connect to monitor server {},may be monitor server is shutdown", RpcMonitorClient.getInstance().getMonitorAddress());
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
                            } while (!RpcMonitorClient.getInstance().isConnected());
                        }
                        try {
                            if (RpcMonitorClient.this.connected) {
                                MonitorInformation information = new MonitorInformation();
                                information.setRpcCallInfo(RpcMonitorClient.this.rpcCallInfo);
                                RpcMonitorClient.this.heartBeat(information);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("Rpc monitor client heart beat task error", e);
                        }

                    }
                }, 1, 10, TimeUnit.SECONDS);
                log.info("Rpc monitor start schedule heat beat thread");
            }

        }

    }
    public synchronized void reconnect() throws Exception{
        log.info("Rpc monitor client reconnect to monitor server {}",this.monitorAddress);
        connect(this.monitorAddress);
        if(this.groups!=null){
            register(this.groups, this.selfPort);
        }
    }
    public String getMonitorHost(){
        return monitorHost;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    
    public Map<String, MonitorNode> getAllNodeMap() throws Exception{
        return serverNode.syncCall(MonitorConstant.MonitorServerMethod.GET_ALL_NODE_MAP,Map.class);
    }

    
    public void register(String[] groups, int selfPort) throws Exception {
        this.groups=groups;
        this.selfPort =selfPort;
        serverNode.syncCall(MonitorConstant.MonitorServerMethod.REGISTER,Void.class,new Object[]{groups,selfPort});
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
    public void addRpcCallRecord(String cmd,long callTime,long cost){
        if(isRecordRpcCall()){
            rpcCallInfo.addRecord(cmd,callTime,cost);
        }

    }

    public String getMonitorAddress() {
        return monitorAddress;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}
