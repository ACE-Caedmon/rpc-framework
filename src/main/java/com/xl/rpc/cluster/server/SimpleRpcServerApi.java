package com.xl.rpc.cluster.server;

import com.xl.rpc.boot.EngineSettings;
import com.xl.rpc.boot.ServerSettings;
import com.xl.rpc.boot.ServerSocketEngine;
import com.xl.rpc.boot.SocketEngine;
import com.xl.rpc.monitor.client.SimpleRpcMonitorApi;
import com.xl.rpc.dispatch.RpcMethodInterceptor;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.dispatch.method.ReflectRpcMethodDispatcher;
import com.xl.rpc.exception.EngineException;
import com.xl.rpc.internal.PrototypeBeanAccess;
import com.xl.utils.PropertyKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by Administrator on 2015/7/15.
 */
public class SimpleRpcServerApi implements RpcServerApi {
    private String host;
    private int port=8001;
    private int bossThreadSize=Runtime.getRuntime().availableProcessors();
    private int workerThreadSize=Runtime.getRuntime().availableProcessors();
    private int cmdThreadSize=Runtime.getRuntime().availableProcessors();
    private String beanAccessClass= PrototypeBeanAccess.class.getName();
    private List<String> scanPackage=new ArrayList<>();
    private String monitorServerAddress;
    private String[] clusterNames;
    private volatile boolean started;
    private ServerSocketEngine socketEngine;
    private BeanAccess beanAccess;
    private static final Logger log= LoggerFactory.getLogger(SimpleRpcServerApi.class);
    public static final String RPC_SERVER_HOST_PROPERTY="rpc.server.host";
    public static final String RPC_SERVER_PORT_PROPERTY="rpc.server.port";
    public static final String BOSS_THREAD_SIZE_PROPERTY="rpc.server.bossThreadSize";
    public static final String WORKER_THREAD_SIZE_PROPERTY="rpc.server.workerThreadSize";
    public static final String CMD_THREAD_SIZE_PROPERTY="rpc.server.cmdThreadSize";
    public static final String RPC_SERVER_CLUSTER_NAMES="rpc.server.clusterNames";
    public static final String SCAN_PACKAGE_NAME_PROPERTY="rpc.server.scanPackage";
    public static final String BEAN_ACCESS_PROPERTY="rpc.server.beanAccessClass";
    public static final String JAVASSIT_WRITE_CLASS="javassit.writeClass";

    public SimpleRpcServerApi(String configPath){
        Properties properties= PropertyKit.loadProperties(configPath);
        init(properties);
    }
    public SimpleRpcServerApi(Properties properties){
        init(properties);
    }
    public void init(Properties properties){
        if(properties.containsKey(JAVASSIT_WRITE_CLASS)){
            boolean writeClass=Boolean.valueOf(properties.getProperty(JAVASSIT_WRITE_CLASS));
            System.setProperty(JAVASSIT_WRITE_CLASS,String.valueOf(writeClass));
        }
        if(properties.containsKey(RPC_SERVER_HOST_PROPERTY)){
            this.host=properties.getProperty(RPC_SERVER_HOST_PROPERTY);
        }
        if(properties.containsKey(RPC_SERVER_PORT_PROPERTY)){
            this.port=Integer.parseInt(properties.getProperty(RPC_SERVER_PORT_PROPERTY));
        }
        if(properties.containsKey(BOSS_THREAD_SIZE_PROPERTY)){
            this.bossThreadSize=Integer.parseInt(properties.getProperty(BOSS_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(BOSS_THREAD_SIZE_PROPERTY)){
            this.bossThreadSize=Integer.parseInt(properties.getProperty(BOSS_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(WORKER_THREAD_SIZE_PROPERTY)){
            this.workerThreadSize=Integer.parseInt(properties.getProperty(WORKER_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(CMD_THREAD_SIZE_PROPERTY)){
            this.cmdThreadSize=Integer.parseInt(properties.getProperty(CMD_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(RPC_SERVER_CLUSTER_NAMES)){
            this.clusterNames =properties.getProperty(RPC_SERVER_CLUSTER_NAMES).split(",");
        }else{
            throw new NullPointerException(RPC_SERVER_CLUSTER_NAMES+" not specified");
        }
        if(properties.containsKey(SCAN_PACKAGE_NAME_PROPERTY)){
            String[] array=properties.getProperty(SCAN_PACKAGE_NAME_PROPERTY).split(",");
            for(String pack:array){
                this.scanPackage.add(pack);
            }
        }else{
            throw new NullPointerException(SCAN_PACKAGE_NAME_PROPERTY+" not specified");
        }
        if(properties.containsKey(BEAN_ACCESS_PROPERTY)){
            this.beanAccessClass=properties.getProperty(BEAN_ACCESS_PROPERTY);
        }
    }
    @Override
    public void bind() {
        ServerSettings settings=new ServerSettings();
        settings.port=this.port;
        settings.protocol= SocketEngine.TCP_PROTOCOL;
        settings.bossThreadSize=this.bossThreadSize;
        settings.workerThreadSize=this.workerThreadSize;
        settings.scanPackage=new String[this.scanPackage.size()];
        this.scanPackage.toArray(settings.scanPackage);
        log.info("SimpleRpcServer scan package {}",Arrays.toString(settings.scanPackage));
        settings.cmdThreadSize=this.cmdThreadSize;
        try{
            this.beanAccess=(BeanAccess)(Class.forName(beanAccessClass).newInstance());
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }
        RpcMethodDispatcher dispatcher=new ReflectRpcMethodDispatcher(beanAccess,cmdThreadSize);
        socketEngine=new ServerSocketEngine(settings,dispatcher);
        try{
            socketEngine.start();
        }catch (Exception e){
            e.printStackTrace();
            log.error("SimpleRpcServer socket engine start error",e);
        }

        setStarted(true);
        log.info("Rpc server bind success:version={}", EngineSettings.VERSION);
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public List<String> getClusterNames() {
        return Arrays.asList(this.clusterNames);
    }

    @Override
    public String getMonitorServerAddress() {
        return this.monitorServerAddress;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    @Override
    public BeanAccess getBeanAccess() {
        return this.beanAccess;
    }

    @Override
    public void addInterceptor(RpcMethodInterceptor interceptor) {
        socketEngine.addCmdMethodInterceptor(interceptor);
    }

    @Override
    public void addScanPackage(String packageName) {
        this.scanPackage.add(packageName);
    }
}
