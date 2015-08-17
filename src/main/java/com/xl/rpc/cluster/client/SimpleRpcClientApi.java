package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.boot.RpcClientSocketEngine;
import com.xl.rpc.dispatch.CmdInterceptor;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;
import com.xl.utils.ClassUtils;
import com.xl.utils.PropertyKit;
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
    private IClusterServerManager serverManager;
    private RpcClientTemplate template;
    private RpcCallProxyFactory rpcCallProxyFactory;
    private static SimpleRpcClientApi instance=new SimpleRpcClientApi();
    private SimpleRpcClientApi(){
    }
    public SimpleRpcClientApi load(String config){
        Properties properties=PropertyKit.loadProperties(config);
        load(properties);
        return this;
    }
    public SimpleRpcClientApi load(Properties properties){
        this.template=new RpcClientTemplate(properties);
        initComponents();
        return this;
    }
    private void initComponents(){
        this.rpcCallProxyFactory=new CglibRpcCallProxyFactory();
        //扫描接口，预加载生成接口代理
        try{
            List<Class> allClasses=ClassUtils.getClasssFromPackage(template.getScanPackage());
            log.info("Scan all classes {}",allClasses.size());
            for(Class clazz:allClasses){
                RpcControl rpcControl= ClassUtils.getAnnotation(clazz,RpcControl.class);
                if(rpcControl!=null&&clazz.isInterface()){
                    String clusterName=rpcControl.value();
                    rpcCallProxyFactory.getRpcCallProxy(true, clazz);
                    log.info("Create rpcCallProxy : clusterName = {},class = {}", clusterName,StringUtil.simpleClassName(clazz));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Scan classes error:",e);
        }
    }




    public static SimpleRpcClientApi getInstance(){
        return instance;
    }

    @Override
    public void bind() {
        serverManager=new ClusterServerManager(template);
        List<String> clusterNames=new ArrayList<>();
        if(template.getMonitorService()!=null){
            for(String s:template.getMonitorService()){
                clusterNames.add(s);
                serverManager.addClusterGroup(s);
                log.info("Zookeeper add monitor service :clusterName = {}",s);
            }
        }

        log.info("SimpleRpcClient bind OK !");
    }


    @Override
    public void asyncRpcCall(String clusterName,String cmd, Class[] paramTypes,Object... content) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            node.asyncCall(cmd, null,paramTypes,content);
        }catch (Exception e){
            log.error("AsyncRpcCall error:server = {},cmd = {}",node.getKey(),cmd,e);
            //重新选择节点重传
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            for(ServerNode activeNode:nodes){
                try{
                    activeNode.asyncCall(cmd,null,paramTypes, content);
                }catch (Exception e1){
                    log.error("AsyncRpcCall retry call error:clusterName = {},server = {},cmd = {}",clusterName,node.getKey(),cmd,e1);
                    continue;
                }
            }
        }
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String cmd, Class<T> resultType,Class[] paramTypes, Object... content) throws TimeoutException{
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        try{
            T result=node.syncCall(cmd, resultType,paramTypes, content);
            return result;
        }catch (Exception e){
            if(e instanceof TimeoutException){
                throw new TimeoutException("Sync rpc call error:server = "+node.getKey()+",cmd = "+cmd);
            }
            log.error("Sync rpc call error:clusterName = {},server = {},cmd = {},params = {}",clusterName,node.getKey(),cmd,content[0],e);
            //重新选择节点重传,重试次数
            List<ServerNode> nodes=serverManager.getGroupByName(clusterName).getNodeList();
            final int retry=nodes.size()>=template.getRetryCount()?template.getRetryCount():nodes.size();
            for(int i=0;i<retry;i++){
                try{
                    ServerNode activeNode=nodes.get(i);
                    return activeNode.syncCall(cmd, resultType,paramTypes, content);
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



    @Override
    public List<ServerNode> getServersByClusterName(String clusterName) {
        return serverManager.getGroupByName(clusterName).getNodeList();
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String address, String cmd, Class<T> resultType, Class[] paramTypes,Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        return node.syncCall(cmd, resultType,paramTypes, params);
    }

    @Override
    public void asyncRpcCall(String clusterName,String address, String cmd,Class[] paramTypes, Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        node.asyncCall(cmd,null,paramTypes, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, String cmd, AsyncRpcCallBack callback,Class[] paramTypes,Object...params) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        node.asyncCall(cmd,callback,paramTypes,params);
    }

    public IClusterServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public <T> T syncHashRpcCall(String clusterName, String key, String cmd, Class<T> resultType,Class[] paramTypes, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node.syncCall(cmd, resultType,paramTypes, params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, String cmd,Class[] paramTypes, Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        node.asyncCall(cmd,null,paramTypes,params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, String cmd, AsyncRpcCallBack callBack,Class[] paramTypes, Object... params) {
        ServerNode node=getShardNode(clusterName,key);
        node.asyncCall(cmd,callBack,paramTypes,params);
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
