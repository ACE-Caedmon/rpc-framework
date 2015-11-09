package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.boot.EngineSettings;
import com.xl.rpc.boot.RpcClientSocketEngine;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.RpcMethodInterceptor;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;
import com.xl.rpc.dispatch.method.RpcCallback;
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
    private List<RpcMethodInterceptor> interceptors=new ArrayList<>();
    private static SimpleRpcClientApi instance=new SimpleRpcClientApi();
    private Map<String,String> routeTable=new HashMap<>();
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
            for(Class clazz:allClasses){
                RpcControl rpcControl= ClassUtils.getAnnotation(clazz,RpcControl.class);
                if(rpcControl!=null&&clazz.isInterface()){
                    String clusterName=rpcControl.value();
                    rpcCallProxyFactory.getRpcCallProxy(true, clazz);
                    log.info("Rpc create call proxy : clusterName = {},class = {}", clusterName,StringUtil.simpleClassName(clazz));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Rpc scan classes error:",e);
        }
    }




    public static SimpleRpcClientApi getInstance(){
        return instance;
    }

    @Override
    public void bind() {
        serverManager=new ClusterServerManager(template,interceptors);
        List<String> clusterNames=new ArrayList<>();
        if(template.getMonitorService()!=null){
            for(String s:template.getMonitorService()){
                clusterNames.add(s);
                serverManager.addClusterGroup(s);
                log.info("Zookeeper add monitor service :clusterName = {}",s);
            }
        }

        log.info("Rpc client bind success:version={}", EngineSettings.VERSION);
    }


    @Override
    public void asyncRpcCall(String clusterName,String cmd,Object... content) {
        ServerNode node=getRouteServer(clusterName,cmd);
        node.asyncCall(cmd, null, content);
    }

    @Override
    public <T> T syncRpcCall(String clusterName, String cmd, Class<T> resultType,Object... params) throws Exception{
        ServerNode node=getRouteServer(clusterName,cmd);
        T result=node.syncCall(cmd, resultType, params);
        return result;
    }

    @Override
    public void asyncRpcCall(String clusterName, String address, String cmd, AsyncRpcCallBack callback, Object... params) throws Exception {
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("No server node exists:server = "+serverKey);
        }
        node.asyncCall(cmd,callback, params);
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
    public <T> T syncRpcCall(String clusterName, String address, String cmd, Class<T> resultType,Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){

            throw new NullPointerException("Rpc server node not exists:server = "+serverKey);
        }
        return node.syncCall(cmd, resultType, params);
    }

    @Override
    public void asyncRpcCall(String clusterName,String address, String cmd, Object... params) throws Exception{
        String serverKey=clusterName+"-"+address;
        ServerNode node=serverManager.getServerNode(serverKey);
        if(node==null){
            throw new NullPointerException("Rpc server node not exists:server = "+serverKey);
        }
        node.asyncCall(cmd, null, params);
    }

    @Override
    public void asyncRpcCall(String clusterName, String cmd, AsyncRpcCallBack callback,Object...params) {
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        node.asyncCall(cmd,callback,params);
    }
    public void asyncRpcCall4Fuse(String clusterName,String cmd, RpcCallback callback,Object[] logParams,Object... params){
        ServerNode node=serverManager.getOptimalServerNode(clusterName);
        String logMessage="Fuse_RpcCall:address="+node.getKey()+",seq={},cmd={},ret ={},uin={},imei={},bsize={}";
        log.debug(logMessage,logParams);
        node.asyncCall(cmd, callback, params);
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
    public void asyncHashRpcCall(String clusterName, String key, String cmd,Object... params) throws Exception {
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        node.asyncCall(cmd,null,params);
    }

    @Override
    public void asyncHashRpcCall(String clusterName, String key, String cmd, AsyncRpcCallBack callBack, Object... params) {
        ServerNode node=getShardNode(clusterName, key);
        node.asyncCall(cmd,callBack,params);
    }
    private ServerNode getShardNode(String clusterName,String key){
        ClusterGroup group=serverManager.getGroupByName(clusterName);
        ServerNode node=group.getShardNode(key);
        return node;
    }

    @Override
    public void addRpcMethodInterceptor(RpcMethodInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public boolean existsServerNode(String clusterName, String address) {
        return serverManager.getServerNode(clusterName+"-"+address)!=null;
    }

    @Override
    public Map<String, String> getRouteTable() {
        return routeTable;
    }

    @Override
    public void setRouteTable(Map<String, String> routeTable) {
        this.routeTable = routeTable;
    }
    public ServerNode getRouteServer(String clusterName,String cmd){
        ServerNode node=null;
        for(String cmdRegex:routeTable.keySet()){
            if(cmd.matches(cmdRegex)){
                String serverKey=routeTable.get(cmd);
                node=serverManager.getServerNode(serverKey);
                log.debug("Rpc client route:{}=>{}",cmd,serverKey);
                break;
            }
        }
        if(node==null){
            node=serverManager.getOptimalServerNode(clusterName);
        }
        return node;
    }
}
