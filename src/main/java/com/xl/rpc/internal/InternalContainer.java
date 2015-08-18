package com.xl.rpc.internal;

import com.xl.rpc.cluster.client.RpcClientApi;
import com.xl.rpc.cluster.client.SimpleRpcClientApi;
import com.xl.rpc.cluster.server.RpcServerApi;
import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import com.xl.utils.PropertyKit;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import java.util.Properties;

/**
 * Created by Administrator on 2015/8/5.
 */
public class InternalContainer {
    private static final InternalContainer instance=new InternalContainer();
    private RpcClientApi rpcClientApi;
    private RpcServerApi rpcServerApi;
    private ClassPathXmlApplicationContext springContext;
    private RunProfile runProfile = RunProfile.debug;
    private InternalContainer(){

    }

    public void setRunProfile(RunProfile runProfile) {
        this.runProfile = runProfile;
    }

    public enum RunProfile {
        debug,product;
    }
    public static InternalContainer getInstance(){
        return instance;
    }
    public synchronized void startRpcClient(Properties... propertiesArray){
        if(this.rpcClientApi==null){
            Properties suitableProperties= getSuitableProperties(propertiesArray);
            this.rpcClientApi=SimpleRpcClientApi.getInstance().load(suitableProperties);
            this.rpcClientApi.bind();
        }
    }
    public synchronized void startRpcServer(Properties... propertiesArray){
        if(this.rpcServerApi==null){
            Properties suitableProperties=getSuitableProperties(propertiesArray);
            this.rpcServerApi=new SimpleRpcServerApi(suitableProperties);
            this.rpcServerApi.bind();
        }
    }
    public synchronized void startRpcClient(String... configs){
        startRpcClient(loadProperties(configs));
    }
    public synchronized void startRpcServer(String... configs){
        if(this.rpcServerApi==null){
            Properties[] propertiesArray=loadProperties(configs);
            startRpcServer(getSuitableProperties(propertiesArray));
        }
    }
    public synchronized void initSpringContext(String... config){
        if(springContext==null){
            springContext=new ClassPathXmlApplicationContext();
            StandardEnvironment environment=new StandardEnvironment();
            environment.addActiveProfile(runProfile.name());
            springContext.setEnvironment(environment);
            springContext.setConfigLocations(config);
            springContext.refresh();
        }
    }
    public  Properties getSuitableProperties(Properties... propertiesArray){
        Properties suitableProperties=null;
        for(Properties properties:propertiesArray){
            if(properties.containsKey("server.profile")){
                String value=properties.getProperty("server.profile");
                if(value.trim().toLowerCase().equals(runProfile.name())){
                    suitableProperties=properties;
                    break;
                }
            }
        }
        if(suitableProperties==null){
            throw new IllegalArgumentException("Do not found suit profile '"+runProfile+"'");
        }
        return suitableProperties;
    }
    public static Properties[] loadProperties(String...configs){
        Properties[] propertiesArray=new Properties[configs.length];
        for(int i=0;i<propertiesArray.length;i++){
            Properties properties= PropertyKit.getProperties(configs[i]);
            propertiesArray[i]=properties;
        }
        return propertiesArray;
    }
    public synchronized void setSpringContext(ClassPathXmlApplicationContext context){
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
}
