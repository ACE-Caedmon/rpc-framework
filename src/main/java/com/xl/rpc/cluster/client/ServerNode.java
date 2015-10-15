package com.xl.rpc.cluster.client;

import com.xl.rpc.boot.RpcClientSocketEngine;
import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.RpcMethodInterceptor;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.ReflectRpcMethodDispatcher;
import com.xl.rpc.dispatch.method.RpcCallback;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.exception.ClusterNodeException;
import com.xl.rpc.exception.EngineException;
import com.xl.session.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2015/7/14.
 */
public class ServerNode implements Comparable<ServerNode>{
    private RpcClientSocketEngine socketEngine;
    private String clusterName;
    private String host;
    private int port;
    private int syncCallNumber;
    private int syncCallTimeout=3;
    private int weight=DEFAULT_WEIGHT;
    public static final int DEFAULT_WEIGHT = 1;
    public long averageResponseTime;
    public long totalResponseTime;
    private static final Logger log= LoggerFactory.getLogger(ServerNode.class);
    public ServerNode(RpcClientSocketEngine socketEngine) {
        if(socketEngine==null){
            throw new NullPointerException("Session can not be null");
        }
        this.socketEngine = socketEngine;
    }

    public ISession getSession() {
        return socketEngine.getSession();
    }

    public RpcClientSocketEngine getSocketEngine(){
        return socketEngine;
    }
    @Override
    public int compareTo(ServerNode o) {
        return this.computeLoad()-o.computeLoad();
    }
    public void asyncCall(String cmd, RpcCallback callback,Object... params){
        RpcPacket packet=new RpcPacket(cmd,params);
        packet.setSync(false);
        getSession().asyncRpcSend(packet, callback);
        log.info("Rpc async call:server = {},cmd ={}",getKey(),cmd);
    }
    public String getKey(){
        return clusterName+"-"+host+":"+port;
    }
    private static String[] buildClassNameArray(Class[] paramTypes){
        String[] classNameArray=new String[paramTypes.length];
        for(int i=0;i<paramTypes.length;i++){
            classNameArray[i]=paramTypes[i].getName();
        }
        return classNameArray;
    }
    public <T> T syncCall(String cmd, Class<T> resultType, Object... params) throws Exception{
        if(!isActive()){
            throw new ClusterNodeException("Rpc node is not active:clusterName = "+clusterName+",server = "+host+":"+port);
        }
        //RpcPacket.validate(paramTypes, params);
        RpcPacket packet=new RpcPacket(cmd,params);
        //packet.setClassNameArray(buildClassNameArray(paramTypes));
        packet.setSync(true);
        long before= System.currentTimeMillis();
        T result=getSession().syncRpcSend(packet, resultType, (long) syncCallTimeout, TimeUnit.SECONDS);
        long after=System.currentTimeMillis();
        syncCallNumber++;
        this.averageResponseTime=(totalResponseTime+(after-before))/syncCallNumber;
        log.info("Rpc sync call:server = {},cmd ={},responseTime = {}",getKey(),cmd,this.averageResponseTime);
        return result;
    }
    public boolean isActive(){
        return getSession().isActive();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerNode)) return false;

        ServerNode that = (ServerNode) o;

        if (port != that.port) return false;
        if (!clusterName.equals(that.clusterName)) return false;
        return host.equals(that.host);

    }

    @Override
    public int hashCode() {
        int result = clusterName.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        return result;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void reconnect(){

    }
    public int computeLoad(){
        return this.syncCallNumber;
    }

    public int getSyncCallTimeout() {
        return syncCallTimeout;
    }

    public void setSyncCallTimeout(int syncCallTimeout) {
        this.syncCallTimeout = syncCallTimeout;
    }
    public void destory(){
        ISession session=getSession();
        if(session!=null){
            session.disconnect(true);
        }

    }
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public long getAverageResponseTime() {
        return averageResponseTime;
    }

    public long getTotalResponseTime() {
        return totalResponseTime;
    }

    public int getSyncCallNumber() {
        return syncCallNumber;
    }

    public static ServerNode  build(String clusterName,String remoteHost,int remotePort,RpcClientTemplate template,List<RpcMethodInterceptor> interceptors) throws Exception{
        TCPClientSettings settings=template.createClientSettings(remoteHost, remotePort);
        BeanAccess beanAccess=null;
        try{
            beanAccess=(BeanAccess)Class.forName(template.getBeanAccessClass()).newInstance();
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }
        RpcMethodDispatcher dispatcher=new ReflectRpcMethodDispatcher(beanAccess,template.getCmdThreadSize());
        RpcClientSocketEngine clientSocketEngine=new RpcClientSocketEngine(settings,dispatcher,template.getLoopGroup());
        if(interceptors!=null){
            for(RpcMethodInterceptor interceptor:interceptors){
                clientSocketEngine.addCmdMethodInterceptor(interceptor);
            }
        }
        clientSocketEngine.start();
        ServerNode serverNode=new ServerNode(clientSocketEngine);
        serverNode.setSyncCallTimeout(template.getCallTimeout());
        serverNode.setHost(remoteHost);
        serverNode.setPort(remotePort);
        serverNode.setSyncCallTimeout(settings.syncTimeout);
        serverNode.setClusterName(clusterName);
        log.info("Rpc create new node:{}", serverNode.getKey());
        return serverNode;
    }
}
