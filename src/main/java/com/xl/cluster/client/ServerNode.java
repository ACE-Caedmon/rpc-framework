package com.xl.cluster.client;

import com.xl.codec.RpcPacket;
import com.xl.dispatch.method.RpcCallback;
import com.xl.exception.ClusterNodeException;
import com.xl.session.ISession;
import com.xl.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2015/7/14.
 */
public class ServerNode implements Comparable<ServerNode>{
    private ISession session;
    private String clusterName;
    private String host;
    private int port;
    private int syncCallNumber;
    private int syncCallTimeout;
    private int weight=DEFAULT_WEIGHT;
    public static final int DEFAULT_WEIGHT = 1;
    public long averageResponseTime;
    public long totalResponseTime;
    private static final Logger log= LoggerFactory.getLogger(ServerNode.class);
    public ServerNode(ISession session) {
        this.session = session;
    }

    public ISession getSession() {
        return session;
    }
    @Override
    public int compareTo(ServerNode o) {
        return this.computeLoad()-o.computeLoad();
    }
    public void asyncCall(int cmd, RpcCallback callback, Object... params){
        RpcPacket packet=new RpcPacket(cmd,params);
        packet.setSync(false);
        session.asyncRpcSend(packet, callback);
        log.info("异步远程调用:server = {},cmd ={}",getKey(),cmd);
    }
    public String getKey(){
        return host+":"+port;
    }
    public <T> T syncCall(int cmd, Class<T> resultType, Object... content) throws Exception{
        if(!isActive()){
            throw new ClusterNodeException("节点状态异常,无法接受信息( value = "+clusterName+",server = "+host+":"+port);
        }

        RpcPacket packet=new RpcPacket(cmd,content);
        packet.setSync(true);
        long before= CommonUtils.now();
        T result=session.syncRpcSend(packet, resultType, (long) syncCallTimeout, TimeUnit.SECONDS);
        long after=CommonUtils.now();
        syncCallNumber++;
        this.averageResponseTime=(totalResponseTime+(after-before))/syncCallNumber;
        log.info("同步远程调用:server = {},cmd ={},responseTime = {}",getKey(),cmd,this.averageResponseTime);
        return result;
    }
    public boolean isActive(){
        return session.isActive();
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

    public void setSession(ISession session) {
        this.session = session;
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
        session.disconnect(true);
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
}
