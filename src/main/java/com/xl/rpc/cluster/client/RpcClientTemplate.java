package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.boot.SocketEngine;
import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.internal.PrototypeBeanAccess;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Properties;

/**
 * Created by Administrator on 2015/8/17.
 */
public class RpcClientTemplate {
    private int callTimeout=10;
    private String beanAccessClass= PrototypeBeanAccess.class.getName();
    private int retryCount=3;
    private int workerThreadSize=Runtime.getRuntime().availableProcessors();
    private int cmdThreadSize= Runtime.getRuntime().availableProcessors();
    private String[] scanPackage=new String[]{""};
    private String lb ="responseTime";
    private EventLoopGroup loopGroup;
    private String[] monitorService;
    private static final String WORK_THREAD_SIZE_PROPERTY="rpc.client.workerThreadSize";
    private static final String CMD_THREAD_SIZE_PROPERTY="rpc.client.cmdThreadSize";
    private static final String CALL_TIME_OUT_PROPERTY="rpc.client.callTimeout";
    private static final String RPC_MSG_TYPE_PROPERTY="rpc.client.msgType";
    private static final String SCAN_PACKAGE_PROPERTY="rpc.client.scanPackage";
    private static final String BEAN_ACCESS_PROPERTY="rpc.client.beanAccessClass";
    private static final String MONITOR_SERVICE_PROPERTY="rpc.client.monitorService";
    private static final String RPC_RETRY_COUNT_PROPERTY="rpc.client.retryCount";
    private static final String LB_PROPERTY ="rpc.client.lb";
    private RpcClientTemplate(){

    }
    public static RpcClientTemplate newDefault(){
        return new RpcClientTemplate();
    }
    public RpcClientTemplate(Properties properties){
        if(properties.containsKey(WORK_THREAD_SIZE_PROPERTY)){
            this.workerThreadSize=Integer.parseInt(properties.getProperty(WORK_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(CMD_THREAD_SIZE_PROPERTY)){
            this.cmdThreadSize=Integer.parseInt(properties.getProperty(CMD_THREAD_SIZE_PROPERTY));
        }
        if(properties.containsKey(CALL_TIME_OUT_PROPERTY)){
            this.callTimeout=Integer.parseInt(properties.getProperty(CALL_TIME_OUT_PROPERTY));
        }
        if(properties.containsKey(RPC_MSG_TYPE_PROPERTY)){
            MsgType msgType=MsgType.valueOf(properties.getProperty(RPC_MSG_TYPE_PROPERTY));
            System.setProperty(RPC_MSG_TYPE_PROPERTY,msgType.name());
        }
        if(properties.containsKey(SCAN_PACKAGE_PROPERTY)){
            this.scanPackage=properties.getProperty(SCAN_PACKAGE_PROPERTY).split(",");
        }else{
            throw new NullPointerException(SCAN_PACKAGE_PROPERTY+" not specified");
        }
        if(properties.containsKey(BEAN_ACCESS_PROPERTY)){
            this.beanAccessClass=properties.getProperty(BEAN_ACCESS_PROPERTY);
        }
        if(properties.containsKey(MONITOR_SERVICE_PROPERTY)){
           this.monitorService=properties.getProperty(MONITOR_SERVICE_PROPERTY).split(",");
        }
        if(properties.containsKey(RPC_RETRY_COUNT_PROPERTY)){
            this.retryCount=Integer.parseInt(properties.getProperty(RPC_RETRY_COUNT_PROPERTY));
        }
        if(properties.containsKey(LB_PROPERTY)){
            this.lb =properties.getProperty(LB_PROPERTY);
        }
    }
    public int getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(int callTimeout) {
        this.callTimeout = callTimeout;
    }

    public String getBeanAccessClass() {
        return beanAccessClass;
    }

    public void setBeanAccessClass(String beanAccessClass) {
        this.beanAccessClass = beanAccessClass;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }


    public int getWorkerThreadSize() {
        return workerThreadSize;
    }

    public void setWorkerThreadSize(int workerThreadSize) {
        this.workerThreadSize = workerThreadSize;
    }

    public int getCmdThreadSize() {
        return cmdThreadSize;
    }

    public void setCmdThreadSize(int cmdThreadSize) {
        this.cmdThreadSize = cmdThreadSize;
    }

    public String[] getScanPackage() {
        return scanPackage;
    }

    public void setScanPackage(String[] scanPackage) {
        this.scanPackage = scanPackage;
    }

    public String getLb() {
        return lb;
    }

    public void setLb(String lb) {
        this.lb = lb;
    }

    public EventLoopGroup getLoopGroup() {
        if(loopGroup==null){
            this.loopGroup=new NioEventLoopGroup(workerThreadSize);
        }
        return loopGroup;
    }

    public void setLoopGroup(EventLoopGroup loopGroup) {
        this.loopGroup = loopGroup;
    }

    public String[] getMonitorService() {
        return monitorService;
    }

    public void setMonitorService(String[] monitorService) {
        this.monitorService = monitorService;
    }
    public TCPClientSettings createClientSettings(String remoteHost, int remotePort){
        TCPClientSettings settings=new TCPClientSettings();
        settings.host=remoteHost;
        settings.port=remotePort;
        settings.protocol= SocketEngine.TCP_PROTOCOL;
        settings.workerThreadSize=this.workerThreadSize;
        settings.cmdThreadSize=this.cmdThreadSize;
        settings.scanPackage=scanPackage;
        settings.syncTimeout= callTimeout;
        return settings;
    }

}
