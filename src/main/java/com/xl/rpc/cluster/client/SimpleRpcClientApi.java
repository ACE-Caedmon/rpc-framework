package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.boot.SocketEngine;
import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.boot.TCPClientSocketEngine;
import com.xl.rpc.cluster.ZkServerManager;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.exception.EngineException;
import com.xl.session.ISession;
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
 * Created by Administrator on 2015/7/15.
 */
public class SimpleRpcClientApi implements RpcClientApi {
    private IClusterServerManager serverManager;
    private static final Logger log= LoggerFactory.getLogger(SimpleRpcClientApi.class);
    private int callTimeout;
    private String[] scanPackage;
    private String beanAccessClass;
    private TCPClientSocketEngine clientSocketEngine;
    private ZkServerManager zkServerManager;
    private String[] monitorService;
    private int retryCount;
    private String rpcCallProxy;
    private static SimpleRpcClientApi instance=new SimpleRpcClientApi();
    private RpcCallProxyFactory rpcCallProxyFactory;
    private EventLoopGroup loopGroup;
    private int workerThreadSize;
    private int cmdThreadSize;
    private String zkServer;
    public static final String JAVASSIT_PROXY="javassit",CGLIB_PROXY="cglib";
    private SimpleRpcClientApi(){

    }
    public SimpleRpcClientApi load(String config){
        Properties properties=PropertyKit.loadProperties(config);
        return load(properties);
    }
    public SimpleRpcClientApi load(Properties properties){
        this.workerThreadSize=Integer.parseInt(properties.getProperty("rpc.client.workerThreadSize"));
        this.cmdThreadSize=Integer.parseInt(properties.getProperty("rpc.client.cmdThreadSize"));
        this.callTimeout =Integer.parseInt(properties.getProperty("rpc.client.callTimeout"));
        MsgType msgType=MsgType.valueOf(properties.getProperty("rpc.client.msgType"));
        System.setProperty("ng.socket.msg.type",msgType.name());
        this.scanPackage=properties.getProperty("rpc.client.scanPackage").split(",");
        this.beanAccessClass=properties.getProperty("rpc.client.beanAccessClass");
        this.monitorService=properties.getProperty("rpc.client.monitorService").split(",");
        this.retryCount=Integer.parseInt(properties.getProperty("rpc.client.retryCount"));
        this.rpcCallProxy=properties.getProperty("rpc.client.rpcCallProxy");
        this.zkServer=properties.getProperty("rpc.client.zkServer");
        if(this.rpcCallProxy.equals(JAVASSIT_PROXY)){
            this.rpcCallProxyFactory=new JavassitRpcCallProxyFactory();
        }else{
            this.rpcCallProxyFactory=new CglibRpcCallProxyFactory();
        }
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
        return this;
    }
    public ServerNode newServerNode(String clusterName,String remoteHost,int remotePort) throws Exception{
        TCPClientSettings settings=new TCPClientSettings();
        settings.host=remoteHost;
        settings.port=remotePort;
        settings.protocol= SocketEngine.TCP_PROTOCOL;
        settings.workerThreadSize=this.workerThreadSize;
        settings.cmdThreadSize=this.cmdThreadSize;
        this.loopGroup=new NioEventLoopGroup(settings.workerThreadSize);
        settings.scanPackage=scanPackage;
        settings.syncTimeout= callTimeout;
        BeanAccess beanAccess=null;
        try{
            beanAccess=(BeanAccess)(Class.forName(beanAccessClass).newInstance());
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(beanAccess,settings.cmdThreadSize);
        clientSocketEngine=new TCPClientSocketEngine(settings,dispatcher,loopGroup);
        clientSocketEngine.start();
        ISession session=clientSocketEngine.getSession();
        ServerNode serverNode=new ServerNode(session);
        serverNode.setHost(remoteHost);
        serverNode.setPort(remotePort);
        serverNode.setSyncCallTimeout(settings.syncTimeout);
        serverNode.setClusterName(clusterName);
        log.info("Create new server:{}",serverNode.getKey());
        return serverNode;
    }
    public static SimpleRpcClientApi getInstance(){
        return instance;
    }
    @Override
    public void bind() {
        log.info("SimpleRpcClient bind ");
        String userDir=System.getProperty("user.dir");
        zkServerManager =new ZkServerManager(zkServer);
        zkServerManager.setListener(new ZkServerManager.ServerConfigListener() {
            @Override
            public void onServerListChanged(String s) {
                SimpleRpcClientApi.this.refreshClusterServers(s);
            }
        });
        serverManager=ClusterServerManager.getInstance();
        List<String> clusterNames=new ArrayList<>();
        for(String s:monitorService){
            clusterNames.add(s);
            serverManager.addClusterGroup(s);
            log.info("Zookeeper add monitor service :clusterName = {}",s);
        }
        try{
            zkServerManager.monitorServiceProviders(clusterNames);
        }catch (Exception e){
            e.printStackTrace();
            log.error("Zookeeper monitor service error",e);
        }

        for(String clusterName: zkServerManager.getAllServerMap().keySet()){
            refreshClusterServers(clusterName);
            if(!clusterNames.contains(clusterName)){
                log.warn("No active server nodes in cluster:{}",clusterName);
            }
        }

        log.info("SimpleRpcClient bind OK !");
    }

    @Override
    public void asyncRpcCall(String clusterName, String cmd, Object... content) {
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
            return;
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
                    log.info("Update server node:server = {} ",serverNode.getKey());
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
    public <T> T syncRpcCall(String clusterName, String serverKey, String cmd, Class<T> resultType, Object... params) throws Exception{
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, String serverKey, String cmd, Object... params) throws Exception{
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
}
