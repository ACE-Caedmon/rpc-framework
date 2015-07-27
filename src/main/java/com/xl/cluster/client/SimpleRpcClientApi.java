package com.xl.cluster.client;

import com.xl.annotation.CmdControl;
import com.xl.annotation.Extension;
import com.xl.annotation.MsgType;
import com.xl.boot.ModuleExtension;
import com.xl.boot.SocketEngine;
import com.xl.boot.TCPClientSettings;
import com.xl.boot.TCPClientSocketEngine;
import com.xl.cluster.ZkServerManager;
import com.xl.dispatch.method.*;
import com.xl.exception.EngineException;
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
    public static final String JAVASSIT_PROXY="javassit",CGLIB_PROXY="cglib";
    private SimpleRpcClientApi(){

    }
    public SimpleRpcClientApi load(String config){
        Properties properties=PropertyKit.loadProperties(config);
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
        if(this.rpcCallProxy.equals(JAVASSIT_PROXY)){
            this.rpcCallProxyFactory=new JavassitRpcCallProxyFactory();
        }else{
            this.rpcCallProxyFactory=new CglibRpcCallProxyFactory();
        }
        //扫描接口，预加载生成接口代理
        try{
            List<Class> allClasses=ClassUtils.getClasssFromPackage(scanPackage);
            for(Class clazz:allClasses){
                if(ClassUtils.hasAnnotation(clazz,CmdControl.class)&&clazz.isInterface()){
                    rpcCallProxyFactory.createRpcCallProxy(clazz);
                    log.info("Create RpcCallProxy : class = {}", StringUtil.simpleClassName(clazz));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return this;
    }
    public ServerNode newServerNode(String remoteHost,int remotePort) throws Exception{
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
            throw new EngineException("初始化BeanAccess异常",e);
        }
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(beanAccess,settings.cmdThreadSize);
        clientSocketEngine=new TCPClientSocketEngine(settings,dispatcher,loopGroup);
        clientSocketEngine.start();
        ISession session=clientSocketEngine.getSession();
        ServerNode serverNode=new ServerNode(session);
        serverNode.setHost(remoteHost);
        serverNode.setPort(remotePort);
        serverNode.setSyncCallTimeout(settings.syncTimeout);
        return serverNode;
    }
    public static SimpleRpcClientApi getInstance(){
        return instance;
    }
    @Override
    public void bind() {
        String userDir=System.getProperty("user.dir");
        zkServerManager =new ZkServerManager(userDir);
        zkServerManager.setListener(new ZkServerManager.ServerConfigListener() {
            @Override
            public void onConfigChanged(String s) {
                System.out.println("集群配置变更:"+s);
            }

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
            log.info("监控集群节点:value = {}",s);
        }
        zkServerManager.monitorServiceProviders(clusterNames);

        for(String clusterName: zkServerManager.getAllServerMap().keySet()){
            refreshClusterServers(clusterName);
        }
    }

    @Override
    public void asyncRpcCall(String clusterName, int cmd, Object... content) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            node.asyncCall(cmd, null,content);
        }catch (Exception e){
            log.error("asyncRpcCall 异常:value = {},server = {},cmd = {}",clusterName,node.getKey(),cmd,e);
            //重新选择节点重传
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            for(ServerNode activeNode:nodes){
                try{
                    activeNode.asyncCall(cmd,null, content);
                }catch (Exception e1){
                    log.error("asyncRpcCall 重发异常:value = {},server = {},cmd = {}",clusterName,node.getKey(),cmd,e1);
                    continue;
                }
            }
        }
    }

    @Override
    public <T> T syncRpcCall(String clusterName, int cmd, Class<T> resultType, Object... content) throws TimeoutException{
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            T result=node.syncCall(cmd, resultType, content);
            return result;
        }catch (Exception e){
            if(e instanceof TimeoutException){
                throw (TimeoutException)e;
            }
            log.error("syncRpcCall 异常:value = {},server = {},cmd = {},params = {}",clusterName,node.getKey(),cmd,content[0],e);
            //重新选择节点重传,重试次数
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            final int retry=nodes.size()>=retryCount?retryCount:nodes.size();
            for(int i=0;i<retry;i++){
                try{
                    ServerNode activeNode=nodes.get(i);
                    return activeNode.syncCall(cmd, resultType, content);
                }catch (Exception e1){
                    log.error("syncRpcCall 重发异常:value = {},server = {},cmd = {},params = {}",clusterName,node.getKey(),cmd,content[0],e1);
                    continue;
                }
            }

        }
        return null;
    }

    @Override
    public <T> T getRemoteCallProxy(Class<T> clazz) {
        return rpcCallProxyFactory.getRpcCallProxy(clazz);
    }
    public void refreshClusterServers(String clusterName){
        List<String> newServers= zkServerManager.getServerList(clusterName);
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        if(group==null){
            return;
        }
        Map<String,ServerNode> oldServers=serverManager.getAllServerNodes();
        //遍历新数据
        for(String serverKey:newServers){
            String[] hostAndPort=serverKey.split(":");
            String remoteHost=hostAndPort[0];
            int remotePort=Integer.parseInt(hostAndPort[1]);
            //如果节点存在于新数据,而老数据中没有，则是一个新的节点，需要新建连接
            if(!oldServers.containsKey(serverKey)){
                try{
                    ServerNode serverNode=newServerNode(remoteHost, remotePort);
                    serverNode.setClusterName(clusterName);
                    serverManager.addServerNode(serverNode);
                    log.info("更新集群节点:clusterName = {},server = {} ",clusterName,serverNode.getKey());
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("更新集群节点异常:clusterName = {},server = {}",clusterName,serverKey,e);
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
                for(String serverKey:newServers){
                    if(oldNode.getKey().equals(serverKey)){
                        needRemove=false;
                        break;
                    }
                }
                if(needRemove){
                    it.remove();
                    serverManager.removeServerNode(oldNode.getKey());
                    log.info("移除集群节点:clusterName = {},server = {}",clusterName,oldNode.getKey());
                }
            }
        }

    }

    @Override
    public List<ServerNode> getServersByClusterName(String clusterName) {
        return serverManager.getGroupByName(clusterName).getNodeList();
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String serverKey, int cmd, Class<T> resultType, Object... params) throws Exception{
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("找不到集群节点:server = "+serverKey);
        }
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, String serverKey, int cmd, Object... params) throws Exception{
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("找不到集群节点:server = "+serverKey);
        }
        node.asyncCall(cmd,null, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, int cmd, AsyncRpcCallBack callback,Object...params) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        node.asyncCall(cmd,callback,params);
    }

    public IClusterServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public <T> T syncHashRpcCall(String clusterName, String key, int cmd, Class<T> resultType, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, int cmd, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        node.asyncCall(cmd,null, params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, int cmd, AsyncRpcCallBack callBack, Object... params) {
        ServerNode node=getShardNode(clusterName,key);
        node.asyncCall(cmd,callBack,params);
    }
    private ServerNode getShardNode(String clusterName,String key){
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node;
    }
}
