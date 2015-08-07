package com.xl.rpc.internal;

import com.xl.rpc.cluster.client.RpcClientApi;
import com.xl.rpc.cluster.client.SimpleRpcClientApi;
import com.xl.rpc.cluster.server.RpcServerApi;
import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import com.xl.utils.PropertyKit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Properties;

/**
 * Created by Administrator on 2015/8/5.
 */
public class InternalContainer {
    private static final InternalContainer instance=new InternalContainer();
    private RpcClientApi rpcClientApi;
    private RpcServerApi rpcServerApi;
    private ApplicationContext springContext;
    private InternalContainer(){

    }
    public static InternalContainer getInstance(){
        return instance;
    }
    public synchronized void startRpcClient(Properties properties){
        if(this.rpcClientApi==null){
            this.rpcClientApi=SimpleRpcClientApi.getInstance().load(properties);
            this.rpcClientApi.bind();
        }
    }
    public synchronized void startRpcServer(Properties properties){
        if(this.rpcServerApi==null){
            this.rpcServerApi=new SimpleRpcServerApi(properties);
            this.rpcServerApi.bind();
        }
    }
    public synchronized void startRpcClient(String config){
        if(this.rpcClientApi==null){
            Properties properties= PropertyKit.getProperties(config);
            startRpcClient(properties);
        }
    }
    public synchronized void startRpcServer(String config){
        if(this.rpcServerApi==null){
            Properties properties= PropertyKit.getProperties(config);
            startRpcServer(properties);
        }
    }
    public synchronized void initSpringContext(String config){
        if(springContext==null){
            springContext=new ClassPathXmlApplicationContext(config);
        }
    }

    public RpcClientApi getRpcClientApi() {
        return rpcClientApi;
    }

    public RpcServerApi getRpcServerApi() {
        return rpcServerApi;
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }
    public <T> T getSyncRemoteCallProxy(Class<T> clazz){
        return this.rpcClientApi.getSyncRemoteCallProxy(clazz);
    }
    public <T> T getAsyncRemoteCallProxy(Class<T> clazz){
        return this.rpcClientApi.getAsyncRemoteCallProxy(clazz);
    }
}
