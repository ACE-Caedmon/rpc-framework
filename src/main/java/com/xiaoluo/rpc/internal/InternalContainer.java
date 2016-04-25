package com.xiaoluo.rpc.internal;

import com.xiaoluo.rpc.cluster.client.RpcClientApi;
import com.xiaoluo.rpc.cluster.client.SimpleRpcClientApi;
import com.xiaoluo.rpc.cluster.server.RpcServerApi;
import com.xiaoluo.rpc.cluster.server.SimpleRpcServerApi;
import com.xiaoluo.rpc.dispatch.method.BeanAccess;
import com.xiaoluo.rpc.registry.client.SimpleRegistryApi;
import com.xiaoluo.utils.PropertyKit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import java.util.Properties;

/**
 * Created by Caedmon on 2015/8/5.
 */
public class InternalContainer {
    private static final InternalContainer instance=new InternalContainer();
    private SimpleRegistryApi rpcMonitorApi;
    private RpcClientApi rpcClientApi;
    private RpcServerApi rpcServerApi;
    private ApplicationContext springContext;
    private Properties properties;
    private InternalContainer() {

    }
    public static InternalContainer getInstance(){
        return instance;
    }
    public synchronized void startRpc(String config){
        Properties properties=PropertyKit.loadProperties(config);
        startRpc(properties);
    }
    public synchronized void startRpcServer(Properties properties){
        if(this.rpcServerApi==null){
            this.rpcServerApi=new SimpleRpcServerApi(properties);
            //添加monitor的扫描包
            this.rpcServerApi.addScanPackage("com.xiaoluo.rpc.registry.client");
            this.rpcServerApi.bind();
        }
    }
    public synchronized void startRpc(Properties properties){
        if(this.properties==null){
            this.properties=properties;
        }
        if(this.rpcServerApi==null){
            this.rpcServerApi=new SimpleRpcServerApi(properties);
            //添加monitor的扫描包
            this.rpcServerApi.addScanPackage("com.xiaoluo.rpc.registry.client");
            this.rpcServerApi.bind();
        }
        if(this.rpcMonitorApi==null){
            this.rpcMonitorApi= SimpleRegistryApi.getInstance();
            rpcMonitorApi.bind(properties);
        }
        if(this.rpcClientApi==null){
            this.rpcClientApi=SimpleRpcClientApi.getInstance().load(properties);
            this.rpcClientApi.bind();
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
