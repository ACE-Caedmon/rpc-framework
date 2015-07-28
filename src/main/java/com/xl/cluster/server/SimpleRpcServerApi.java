package com.xl.cluster.server;

import com.xl.boot.ServerSettings;
import com.xl.boot.ServerSocketEngine;
import com.xl.boot.SocketEngine;
import com.xl.cluster.ZkServerManager;
import com.xl.dispatch.method.BeanAccess;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.exception.ClusterException;
import com.xl.exception.EngineException;
import com.xl.utils.PropertyKit;

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
    private String clusterName;
    private String[] scanPackage;
    private String beanAccessClass;
    private String zkServer;
    private ServerSocketEngine socketEngine;
    public SimpleRpcServerApi(String configPath){
        Properties properties= PropertyKit.loadProperties(configPath);
        this.host=properties.getProperty("rpc.server.host");
        this.port=Integer.parseInt(properties.getProperty("rpc.server.port"));
        this.bossThreadSize=Integer.parseInt(properties.getProperty("rpc.server.bossThreadSize"));
        this.workerThreadSize=Integer.parseInt(properties.getProperty("rpc.server.workerThreadSize"));
        this.cmdThreadSize=Integer.parseInt(properties.getProperty("rpc.server.cmdThreadSize"));
        this.clusterName=properties.getProperty("rpc.server.clusterName");
        this.scanPackage=properties.getProperty("rpc.server.scanPackage").split(",");
        this.beanAccessClass=properties.getProperty("rpc.server.beanAccessClass");
        this.zkServer=properties.getProperty("rpc.server.zkServer");
    }
    @Override
    public void bind() {
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
            throw new EngineException("初始化BeanAccess异常",e);
        }
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(beanAccess,cmdThreadSize);
        socketEngine=new ServerSocketEngine(settings,dispatcher);
        socketEngine.start();
        String userDir=System.getProperty("user.dir");
        zkServerManager =new ZkServerManager(this.zkServer,userDir);
        try{
            zkServerManager.registerService(this.clusterName,host+":"+port);
        }catch (Exception e){
            throw new ClusterException("注册服务节点异常",e);
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
}
