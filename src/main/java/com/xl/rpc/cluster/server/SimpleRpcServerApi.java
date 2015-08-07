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
import com.xl.utils.PropertyKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by Administrator on 2015/7/15.
 */
public class SimpleRpcServerApi implements RpcServerApi {
    private ZkServerManager zkServerManager;
    private String host;
    private int port;
    private int bossThreadSize;
    private int workerThreadSize;
    private int cmdThreadSize;
    private String[] clusterNames;
    private String[] scanPackage;
    private String beanAccessClass;
    private String zkServer;
    private ServerSocketEngine socketEngine;
    private static final Logger log= LoggerFactory.getLogger(SimpleRpcServerApi.class);
    public SimpleRpcServerApi(String configPath){
        Properties properties= PropertyKit.loadProperties(configPath);
        init(properties);
    }
    public SimpleRpcServerApi(Properties properties){
        init(properties);
    }
    public void init(Properties properties){
        this.host=properties.getProperty("rpc.server.host");
        this.port=Integer.parseInt(properties.getProperty("rpc.server.port"));
        this.bossThreadSize=Integer.parseInt(properties.getProperty("rpc.server.bossThreadSize"));
        this.workerThreadSize=Integer.parseInt(properties.getProperty("rpc.server.workerThreadSize"));
        this.cmdThreadSize=Integer.parseInt(properties.getProperty("rpc.server.cmdThreadSize"));
        this.clusterNames =properties.getProperty("rpc.server.clusterName").split(",");
        this.scanPackage=properties.getProperty("rpc.server.scanPackage").split(",");
        this.beanAccessClass=properties.getProperty("rpc.server.beanAccessClass");
        this.zkServer=properties.getProperty("rpc.server.zkServer");
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
                zkServerManager.registerService(clusterName,host+":"+port);
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
