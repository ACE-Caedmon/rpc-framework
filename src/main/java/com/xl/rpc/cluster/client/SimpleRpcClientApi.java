package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.boot.RpcClientSocketEngine;
import com.xl.rpc.boot.SocketEngine;
import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.cluster.ZkServiceDiscovery;
import com.xl.rpc.dispatch.CmdInterceptor;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.exception.EngineException;
import com.xl.rpc.internal.PrototypeBeanAccess;
import com.xl.utils.ClassUtils;
import com.xl.utils.PropertyKit;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by Caedmon on 2015/7/15.
 */
public class SimpleRpcClientApi implements RpcClientApi {
    private static final Logger log= LoggerFactory.getLogger(SimpleRpcClientApi.class);
    private int callTimeout=3;
    private String beanAccessClass= PrototypeBeanAccess.class.getName();
    private int retryCount=3;
    private RpcCallProxyFactory rpcCallProxyFactory;
    private int workerThreadSize=Runtime.getRuntime().availableProcessors();
    private int cmdThreadSize= Runtime.getRuntime().availableProcessors();
    private String[] scanPackage=new String[]{""};
    private String zookeeperAddress;
    private IClusterServerManager serverManager;
    private EventLoopGroup loopGroup;
    private ZkServiceDiscovery zkServerManager;
    private String[] monitorService;
    private String loadBalancing="responseTime";
    private static SimpleRpcClientApi instance=new SimpleRpcClientApi();
    private static final String WORK_THREAD_SIZE_PROPERTY="rpc.client.workerThreadSize";
    private static final String CMD_THREAD_SIZE_PROPERTY="rpc.client.cmdThreadSize";
    private static final String CALL_TIME_OUT_PROPERTY="rpc.client.callTimeout";
    private static final String RPC_MSG_TYPE_PROPERTY="rpc.client.msgType";
    private static final String SCAN_PACKAGE_PROPERTY="rpc.client.scanPackage";
    private static final String BEAN_ACCESS_PROPERTY="rpc.client.beanAccessClass";
    private static final String MONITOR_SERVICE_PROPERTY="rpc.client.monitorService";
    private static final String RPC_RETRY_COUNT_PROPERTY="rpc.client.retryCount";
    private static final String ZK_SERVER_ADDRESS_PROPERTY ="rpc.zookeeper.address";
    private static final String LOAD_BALANCING_PROPERTY="rpc.client.loadBalancing";
    private static final String JAVASSIT_WRITE_CLASS="javassit.writeClass";
    private SimpleRpcClientApi(){

    }
    public SimpleRpcClientApi load(String config){
        Properties properties=PropertyKit.loadProperties(config);
        initProperties(properties);
        initComponents();
        return this;
    }
    public SimpleRpcClientApi load(Properties properties){
        initProperties(properties);
        initComponents();
        return this;
    }
    private void initComponents(){
        BeanAccess beanAccess=null;
        try{
            beanAccess=(BeanAccess)(Class.forName(beanAccessClass).newInstance());
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }

        this.rpcCallProxyFactory=new CglibRpcCallProxyFactory();
        //扫描接口，预加载生成接口代理
        try{
            List<Class> allClasses=ClassUtils.getClasssFromPackage(scanPackage);
            log.info("Scan all classes {}",allClasses.size());
            for(Class clazz:allClasses){
                if(ClassUtils.hasAnnotation(clazz,RpcControl.class)&&clazz.isInterface()){
                    rpcCallProxyFactory.getRpcCallProxy(true,clazz);
                    log.info("Create rpcCallProxy : class = {}", StringUtil.simpleClassName(clazz));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Scan classes error:",e);
        }
    }
    private void initProperties(Properties properties){
        if(properties.containsKey(JAVASSIT_WRITE_CLASS)){
            boolean writeClass=Boolean.valueOf(properties.getProperty(JAVASSIT_WRITE_CLASS));
            System.setProperty(JAVASSIT_WRITE_CLASS,String.valueOf(writeClass));
        }
        if(properties.containsKey(WORK_THREAD_SIZE_PROPERTY)){
            this.workerThreadSize=Integer.parseInt(properties.getProperty(WORK_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(CMD_THREAD_SIZE_PROPERTY)){
            this.cmdThreadSize=Integer.parseInt(properties.getProperty(CMD_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(CALL_TIME_OUT_PROPERTY)){
            this.callTimeout =Integer.parseInt(properties.getProperty(CALL_TIME_OUT_PROPERTY));
        }
        if(properties.containsKey(RPC_MSG_TYPE_PROPERTY)){
            MsgType msgType=MsgType.valueOf(properties.getProperty(RPC_MSG_TYPE_PROPERTY));
            System.setProperty(RPC_MSG_TYPE_PROPERTY,msgType.name());
        }
        if(properties.containsKey(SCAN_PACKAGE_PROPERTY)){
            this.scanPackage=properties.getProperty(SCAN_PACKAGE_PROPERTY).split(",");
        }else{
            throw new NullPointerException(SCAN_PACKAGE_PROPERTY+" not specified");
        }
        if(properties.containsKey(BEAN_ACCESS_PROPERTY)){
            this.beanAccessClass=properties.getProperty(BEAN_ACCESS_PROPERTY);
        }
        if(properties.containsKey(MONITOR_SERVICE_PROPERTY)){
            this.monitorService=properties.getProperty(MONITOR_SERVICE_PROPERTY).split(",");
        }
        if(properties.containsKey(RPC_RETRY_COUNT_PROPERTY)){
            this.retryCount=Integer.parseInt(properties.getProperty(RPC_RETRY_COUNT_PROPERTY));
        }
        if(properties.containsKey(ZK_SERVER_ADDRESS_PROPERTY)){
            this.zookeeperAddress =properties.getProperty(ZK_SERVER_ADDRESS_PROPERTY);
        }else{
            throw new NullPointerException(ZK_SERVER_ADDRESS_PROPERTY +"not specified");
        }
        if(properties.containsKey(LOAD_BALANCING_PROPERTY)){
            this.loadBalancing=properties.getProperty(LOAD_BALANCING_PROPERTY);
        }
    }
    public TCPClientSettings createClientSettings(String remoteHost, int remotePort){
        TCPClientSettings settings=new TCPClientSettings();
        settings.host=remoteHost;
        settings.port=remotePort;
        settings.protocol= SocketEngine.TCP_PROTOCOL;
        settings.workerThreadSize=this.workerThreadSize;
        settings.cmdThreadSize=this.cmdThreadSize;
        settings.scanPackage=scanPackage;
        settings.syncTimeout= callTimeout;
        return settings;
    }

    public ServerNode newServerNode(String clusterName,String remoteHost,int remotePort) throws Exception{
        TCPClientSettings settings= createClientSettings(remoteHost, remotePort);
        this.loopGroup=new NioEventLoopGroup(settings.workerThreadSize);
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher((BeanAccess)Class.forName(this.beanAccessClass).newInstance(),this.cmdThreadSize);
        RpcClientSocketEngine clientSocketEngine=new RpcClientSocketEngine(settings,dispatcher,loopGroup);
        clientSocketEngine.start();
        ServerNode serverNode=new ServerNode(clientSocketEngine);
        serverNode.setHost(remoteHost);
        serverNode.setPort(remotePort);
        serverNode.setSyncCallTimeout(settings.syncTimeout);
        serverNode.setClusterName(clusterName);
        log.info("Create new server:{}", serverNode.getKey());
        return serverNode;
    }
    public static SimpleRpcClientApi getInstance(){
        return instance;
    }

    @Override
    public void bind() {
        log.info("SimpleRpcClient bind ");
        zkServerManager =new ZkServiceDiscovery(zookeeperAddress);
        zkServerManager.setListener(new ZkServiceDiscovery.ServerDiscoveryListener() {
            @Override
            public void onServerListChanged(String s) {
                SimpleRpcClientApi.this.refreshClusterServers(s);
            }
        });
        serverManager=ClusterServerManager.getInstance();
        List<String> clusterNames=new ArrayList<>();
        if(monitorService!=null){
            for(String s:monitorService){
                clusterNames.add(s);
                serverManager.addClusterGroup(s);
                log.info("Zookeeper add monitor service :clusterName = {}",s);
            }
        }

        for(String clusterName: zkServerManager.getAllServerMap().keySet()){
            refreshClusterServers(clusterName);
            if(!clusterNames.contains(clusterName)){
                log.warn("No active server nodes in cluster:'{}'",clusterName);
            }
        }

        log.info("SimpleRpcClient bind OK !");
    }


    @Override
    public void asyncRpcCall(String clusterName,String cmd, Object... content) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            node.asyncCall(cmd, null,content);
        }catch (Exception e){
            log.error("AsyncRpcCall error:server = {},cmd = {}",node.getKey(),cmd,e);
            //重新选择节点重传
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            for(ServerNode activeNode:nodes){
                try{
                    activeNode.asyncCall(cmd,null, content);
                }catch (Exception e1){
                    log.error("AsyncRpcCall retry call error:clusterName = {},server = {},cmd = {}",clusterName,node.getKey(),cmd,e1);
                    continue;
                }
            }
        }
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String cmd, Class<T> resultType, Object... content) throws TimeoutException{
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            T result=node.syncCall(cmd, resultType, content);
            return result;
        }catch (Exception e){
            if(e instanceof TimeoutException){
                throw new TimeoutException("Sync rpc call error:server = "+node.getKey()+",cmd = "+cmd);
            }
            log.error("Sync rpc call error:clusterName = {},server = {},cmd = {},params = {}",clusterName,node.getKey(),cmd,content[0],e);
            //重新选择节点重传,重试次数
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            final int retry=nodes.size()>=retryCount?retryCount:nodes.size();
            for(int i=0;i<retry;i++){
                try{
                    ServerNode activeNode=nodes.get(i);
                    return activeNode.syncCall(cmd, resultType, content);
                }catch (Exception e1){
                    log.error("Sync rpc retry call error:clusterName = {},server = {},cmd = {},params = {}",clusterName,node.getKey(),cmd,content[0],e1);
                    continue;
                }
            }

        }
        return null;
    }

    @Override
    public <T> T getSyncRemoteCallProxy(Class<T> clazz) {
        return rpcCallProxyFactory.getRpcCallProxy(true,clazz);
    }

    @Override
    public <T> T getAsyncRemoteCallProxy(Class<T> clazz) {
        return rpcCallProxyFactory.getRpcCallProxy(false,clazz);
    }

    public void refreshClusterServers(String clusterName){
        List<String> newServerAddressList= zkServerManager.getServerList(clusterName);
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        if(group==null){
            group=serverManager.addClusterGroup(clusterName);
        }
        Map<String,ServerNode> oldServers=serverManager.getAllServerNodes();
        //遍历新数据
        for(String address:newServerAddressList){
            String[] hostAndPort=address.split(":");
            String remoteHost=hostAndPort[0];
            int remotePort=Integer.parseInt(hostAndPort[1]);
            //如果节点存在于新数据,而老数据中没有，则是一个新的节点，需要新建连接
            if(!oldServers.containsKey(clusterName+"-"+address)){
                try{
                    ServerNode serverNode=newServerNode(clusterName,remoteHost, remotePort);
                    serverNode.setClusterName(clusterName);
                    serverManager.addServerNode(serverNode);
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("Update server node error:clusterName = {},server = {}",clusterName,address,e);
                }
                continue;
            }
        }

        List<ServerNode> oldServerNodes=group.getNodeList();
        if(oldServerNodes!=null){
            Iterator<ServerNode> it=oldServerNodes.iterator();
            while(it.hasNext()){
                ServerNode oldNode=it.next();
                //如果节点存在于旧数据,而新数据中没有，则节点需要移除，需要销毁
                boolean needRemove=true;
                for(String address:newServerAddressList){
                    if(oldNode.getKey().equals(clusterName+"-"+address)){
                        needRemove=false;
                        break;
                    }
                }
                if(needRemove){
                    it.remove();
                    serverManager.removeServerNode(oldNode.getKey());
                    log.info("Remove server node :server = {}",oldNode.getKey());
                }
            }
        }

    }

    @Override
    public List<ServerNode> getServersByClusterName(String clusterName) {
        return serverManager.getGroupByName(clusterName).getNodeList();
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String address, String cmd, Class<T> resultType, Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncRpcCall(String clusterName,String address, String cmd, Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        node.asyncCall(cmd,null, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, String cmd, AsyncRpcCallBack callback,Object...params) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        node.asyncCall(cmd,callback,params);
    }

    public IClusterServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public <T> T syncHashRpcCall(String clusterName, String key, String cmd, Class<T> resultType, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, String cmd, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        node.asyncCall(cmd,null, params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, String cmd, AsyncRpcCallBack callBack, Object... params) {
        ServerNode node=getShardNode(clusterName,key);
        node.asyncCall(cmd,callBack,params);
    }
    private ServerNode getShardNode(String clusterName,String key){
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node;
    }

    @Override
    public void addRpcMethodInterceptor(CmdInterceptor interceptor) {
        for(Map.Entry<String,ServerNode> entry:serverManager.getAllServerNodes().entrySet()){
            RpcClientSocketEngine socketEngine=entry.getValue().getSocketEngine();
            socketEngine.addRpcMethodInterceptor(interceptor);
        }
    }
}
