package com.xl.rpc.cluster.server;

import com.xl.rpc.boot.ServerSettings;
import com.xl.rpc.boot.ServerSocketEngine;
import com.xl.rpc.boot.SocketEngine;
import com.xl.rpc.cluster.ZkServerManager;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.rpc.exception.ClusterException;
import com.xl.rpc.exception.EngineException;
import com.xl.rpc.internal.PrototypeBeanAccess;
import com.xl.utils.EngineParams;
import com.xl.utils.PropertyKit;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by Administrator on 2015/7/15.
 */
public class SimpleRpcServerApi implements RpcServerApi {
    private ZkServerManager zkServerManager;
    private String host="127.0.0.1";
    private int port=8001;
    private int bossThreadSize=Runtime.getRuntime().availableProcessors();
    private int workerThreadSize=Runtime.getRuntime().availableProcessors();
    private int cmdThreadSize=Runtime.getRuntime().availableProcessors();
    private String beanAccessClass= PrototypeBeanAccess.class.getName();
    private String[] scanPackage=new String[]{""};
    private String zkServer;
    private String[] clusterNames;
    private ServerSocketEngine socketEngine;
    private static final Logger log= LoggerFactory.getLogger(SimpleRpcServerApi.class);
    private static final String RPC_SERVER_HOST_PROPERTY="rpc.server.host";
    private static final String RPC_SERVER_PORT_PROPERTY="rpc.server.port";
    private static final String BOSS_THREAD_SIZE_PROPERTY="rpc.server.bossThreadSize";
    private static final String WORKER_THREAD_SIZE_PROPERTY="rpc.server.workerThreadSize";
    private static final String CMD_THREAD_SIZE_PROPERTY="rpc.server.cmdThreadSize";
    private static final String RPC_SERVER_CLUSTER_NAMES="rpc.server.clusterNames";
    private static final String SCAN_PACKAGE_NAME_PROPERTY="rpc.server.scanPackage";
    private static final String BEAN_ACCESS_PROPERTY="rpc.server.beanAccessClass";
    private static final String ZK_SERVER_ADDRESS="rpc.server.zkServer";
    private static final String JAVASSIT_WRITE_CLASS="javassit.writeClass";
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
            this.scanPackage=properties.getProperty(SCAN_PACKAGE_NAME_PROPERTY).split(",");
        }else{
            throw new NullPointerException(SCAN_PACKAGE_NAME_PROPERTY+" not specified");
        }
        if(properties.containsKey(BEAN_ACCESS_PROPERTY)){
            this.beanAccessClass=properties.getProperty(BEAN_ACCESS_PROPERTY);
        }
        if(properties.containsKey(ZK_SERVER_ADDRESS)){
            this.zkServer=properties.getProperty(ZK_SERVER_ADDRESS);
        }else{
            throw new NullPointerException(ZK_SERVER_ADDRESS+" not specified");
        }

    }
    @Override
    public void bind() {
        log.info("SimpleRpcServer init");
        ServerSettings settings=new ServerSettings();
        settings.port=this.port;
        settings.protocol= SocketEngine.TCP_PROTOCOL;
        settings.bossThreadSize=this.bossThreadSize;
        settings.workerThreadSize=this.workerThreadSize;
        settings.scanPackage=this.scanPackage;
        settings.cmdThreadSize=this.cmdThreadSize;
        BeanAccess beanAccess=null;
        try{
            beanAccess=(BeanAccess)(Class.forName(beanAccessClass).newInstance());
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(beanAccess,cmdThreadSize);
        socketEngine=new ServerSocketEngine(settings,dispatcher);
        socketEngine.start();
        zkServerManager =new ZkServerManager(this.zkServer);
        try{
            for(String clusterName:clusterNames){
                zkServerManager.registerService(clusterName,host,port);
            }

        }catch (Exception e){
            throw new ClusterException("Register cluster service error: clusterNames = "+ clusterNames,e);
        }

    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public String getSelfClusterName() {
        return this.clusterNames[0];
    }
}
