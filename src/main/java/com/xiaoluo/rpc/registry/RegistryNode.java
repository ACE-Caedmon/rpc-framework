package com.xiaoluo.rpc.registry;

import com.xiaoluo.rpc.cluster.client.RpcClientTemplate;
import com.xiaoluo.rpc.cluster.client.ServerNode;
import com.xiaoluo.rpc.registry.event.RegistryEvent;
import com.xiaoluo.rpc.dispatch.ISession;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Administrator on 2015/9/22.
 */
public class RegistryNode {
    private String group;
    private String host;
    private int port;
    private volatile boolean active;
    private long lastActiveTime;
    private RegistryNodeInformation registryNodeInformation;
    private String key;
    private Set<String> bindConfigKeySet=new HashSet<>();
    //RpcServer调用MonitorServer的客户端连接
    private transient ISession session;
    //MonitorServerr作为RpcServer的客户端连接
    private transient ServerNode notifyNode;
    private Properties routeTable=new Properties();
    private static RpcClientTemplate shareClientTemplate=RpcClientTemplate.newDefault();
    private static final Logger log= LoggerFactory.getLogger(RegistryNode.class);
    public static final AttributeKey<String> RPC_NODE_KEY=new AttributeKey<>("RPC_NODE_KEY");
    public static final AttributeKey<Boolean> RPC_MONITOR_CLIENT=new AttributeKey<>("RPC_MONITOR_CLIENT");
    static {
        shareClientTemplate.setScanPackage(new String[]{"com.xiaoluo"});
    }
    public RegistryNode(){

    }
    public void reconnect(ISession session) throws Exception{
        this.key=getKey();
        this.session=session;
        session.setAttribute(RegistryNode.RPC_NODE_KEY, getKey());
        renewNotifyNode();
        this.active=true;
        log.info("Monitor client connect:{}", this.key);
    }
    public RegistryNode(ISession session, String group, String host, int port) throws Exception{
        this.session=session;
        this.group=group;
        this.host=host;
        this.port=port;
        this.key=group+"-"+host+":"+port;
        this.active=true;
        this.lastActiveTime=System.currentTimeMillis();
        session.setAttribute(RegistryNode.RPC_NODE_KEY, getKey());
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

    public RegistryNodeInformation getRegistryNodeInformation() {
        return registryNodeInformation;
    }

    public void setRegistryNodeInformation(RegistryNodeInformation registryNodeInformation) {
        this.registryNodeInformation = registryNodeInformation;
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
        final StringBuilder sb = new StringBuilder("RegistryNode{");
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
        log.info("Disconnect registry node success:{}",key);
    }
    public static RegistryNode build(ISession session, String group, String host, int port) throws Exception{
        RegistryNode node=new RegistryNode(session,group,host,port);

        return node;
    }
    public void notifyEvent(RegistryEvent event){
        if(notifyNode!=null&&notifyNode.isActive()){
            notifyNode.asyncCall(RegistryConstant.MonitorClientMethod.MONITOR_EVENT, null, event);
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
