package com.xl.rpc.internal;

import com.xl.rpc.cluster.client.RpcClientApi;
import com.xl.rpc.cluster.client.SimpleRpcClientApi;
import com.xl.rpc.cluster.server.RpcServerApi;
import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.monitor.client.SimpleRpcMonitorApi;
import com.xl.utils.PropertyKit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import java.util.Properties;

/**
 * Created by Caedmon on 2015/8/5.
 */
public class InternalContainer {
    private static final InternalContainer instance=new InternalContainer();
    private SimpleRpcMonitorApi rpcMonitorApi;
    private RpcClientApi rpcClientApi;
    private RpcServerApi rpcServerApi;
    private ApplicationContext springContext;
    private InternalContainer() {

    }
    public static InternalContainer getInstance(){
        return instance;
    }
    public synchronized void startRpcMonitorClient(Properties properties){
        if(this.rpcMonitorApi==null){
            this.rpcMonitorApi=SimpleRpcMonitorApi.getInstance();
            rpcMonitorApi.bind(properties);
        }
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
            //添加monitor的扫描包
            this.rpcServerApi.addScanPackage("com.xl.rpc.monitor.client");
            this.rpcServerApi.bind();
        }
    }
    public synchronized void startRpcClient(String config){
        Properties properties=PropertyKit.getProperties(config);
        startRpcClient(properties);
    }
    public synchronized void startRpc(Properties properties){
        startRpcServer(properties);
        startRpcMonitorClient(properties);
        startRpcClient(properties);

    }
    public synchronized void startRpcServer(String config){
        if(this.rpcServerApi==null){
            startRpcServer(PropertyKit.getProperties(config));
        }
    }
    public synchronized void initSpringContext(String activeProfile,String... config){
        if(springContext==null){
            final ClassPathXmlApplicationContext classPathApplicationContext=new ClassPathXmlApplicationContext();
            StandardEnvironment environment=new StandardEnvironment();
            environment.addActiveProfile(activeProfile);
            classPathApplicationContext.setEnvironment(environment);
            classPathApplicationContext.setConfigLocations(config);
            classPathApplicationContext.refresh();
            this.springContext=classPathApplicationContext;
        }
    }

    public synchronized void initSingleSpringContext(String config){
        if(springContext==null){
            springContext=new ClassPathXmlApplicationContext(config);
        }
    }

    public synchronized void setSpringContext(ApplicationContext context){
        this.springContext=context;
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
    public BeanAccess getRpcServerBeanAccess(){
        return this.rpcServerApi.getBeanAccess();
    }
}
