package com.xl.rpc.monitor;

import com.xl.rpc.cluster.client.RpcClientTemplate;
import com.xl.rpc.cluster.client.ServerNode;
import com.xl.rpc.monitor.event.ConfigEvent;
import com.xl.rpc.monitor.event.MonitorEvent;
import com.xl.rpc.monitor.server.MonitorManager;
import com.xl.session.ISession;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Administrator on 2015/9/22.
 */
public class MonitorNode {
    private String group;
    private String host;
    private int port;
    private volatile boolean active;
    private long lastActiveTime;
    private MonitorInformation monitorInformation;
    private String key;
    private Set<String> bindConfigKeySet=new HashSet<>();
    //RpcServer调用MonitorServer的客户端连接
    private transient ISession session;
    //MonitorServerr作为RpcServer的客户端连接
    private transient ServerNode notifyNode;
    private Properties routeTable=new Properties();
    private static RpcClientTemplate shareClientTemplate=RpcClientTemplate.newDefault();
    private static final Logger log= LoggerFactory.getLogger(MonitorNode.class);
    public static final AttributeKey<String> RPC_NODE_KEY=new AttributeKey<>("RPC_NODE_KEY");
    public static final AttributeKey<Boolean> RPC_MONITOR_CLIENT=new AttributeKey<>("RPC_MONITOR_CLIENT");
    static {
        shareClientTemplate.setScanPackage(new String[]{"com.xl"});
    }
    public MonitorNode(){

    }
    public void reconnect(ISession session) throws Exception{
        this.key=getKey();
        this.session=session;
        session.setAttribute(MonitorNode.RPC_NODE_KEY, getKey());
        renewNotifyNode();
        this.active=true;
        log.info("Monitor client connect:{}", this.key);
    }
    public MonitorNode(ISession session, String group, String host, int port) throws Exception{
        this.session=session;
        this.group=group;
        this.host=host;
        this.port=port;
        this.key=group+"-"+host+":"+port;
        this.active=true;
        this.lastActiveTime=System.currentTimeMillis();
        session.setAttribute(MonitorNode.RPC_NODE_KEY, getKey());
        renewNotifyNode();

    }
    public void renewNotifyNode() throws Exception{
        if(this.notifyNode==null){
            this.notifyNode =ServerNode.build(this.getGroup(), host, port,shareClientTemplate,null );
        }else{
            this.notifyNode.connect();
        }

    }
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public MonitorInformation getMonitorInformation() {
        return monitorInformation;
    }

    public void setMonitorInformation(MonitorInformation monitorInformation) {
        this.monitorInformation = monitorInformation;
    }

    public Set<String> getBindConfigKeySet() {
        return bindConfigKeySet;
    }
    public String getKey() {
        return group+"-"+host+":"+port;
    }

    public void setNotifyNode(ServerNode notifyNode) {
        this.notifyNode = notifyNode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MonitorNode{");
        sb.append("group='").append(group).append('\'');
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", active=").append(active);
        sb.append('}');
        return sb.toString();
    }

    public ISession getSession() {
        return session;
    }

    public void setSession(ISession session) {
        this.session = session;
    }

    public void disconnect(){
        if(session!=null){
            session.disconnect(true);
        }
        if(notifyNode==null){
            log.warn("Notify node is null:{}",this.getKey());
        }else{
            notifyNode.destory();
        }
        log.info("Disconnect monitor node success:{}",key);
    }
    public static MonitorNode build(ISession session,String group,String host,int port) throws Exception{
        MonitorNode node=new MonitorNode(session,group,host,port);

        return node;
    }
    public void notifyEvent(MonitorEvent event){
        if(notifyNode!=null&&notifyNode.isActive()){
            notifyNode.asyncCall(MonitorConstant.MonitorClientMethod.MONITOR_EVENT, null, event);
            log.info("Notify event to node:address={},event={}", key, event.getType());
        }

    }

    public ServerNode getNotifyNode() {
        return notifyNode;
    }
    public void setBindConfigKeySet(Set<String> bindConfigKeySet) {
        this.bindConfigKeySet = bindConfigKeySet;

    }

    public Properties getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(Properties routeTable) {
        this.routeTable = routeTable;
    }
}
