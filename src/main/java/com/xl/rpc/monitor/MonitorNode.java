package com.xl.rpc.monitor;

import com.xl.rpc.cluster.client.RpcClientTemplate;
import com.xl.rpc.cluster.client.ServerNode;
import com.xl.rpc.monitor.event.ConfigEvent;
import com.xl.rpc.monitor.event.NodeEvent;
import com.xl.rpc.monitor.server.MonitorManager;
import com.xl.session.ISession;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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
    private Set<String> bindConfigKeySet;
    //RpcServer作为RpcCenter的客户端连接
    private transient ISession session;
    //RpcCenter作为RpcServer的客户端连接
    private transient ServerNode notifyNode;
    private static RpcClientTemplate shareClientTemplate=RpcClientTemplate.newDefault();
    private static final Logger log= LoggerFactory.getLogger(MonitorNode.class);
    public static final AttributeKey<String> RPC_NODE_KEY=new AttributeKey<>("RPC_NODE_KEY");
    public static final AttributeKey<Boolean> RPC_MONITOR_CLIENT=new AttributeKey<>("RPC_MONITOR_CLIENT");
    static {
        shareClientTemplate.setScanPackage(new String[]{"com.xl"});
    }
    public MonitorNode(){

    }
    public MonitorNode(ISession session, String group, String host, int port) throws Exception{
        this.session=session;
        this.group=group;
        this.host=host;
        this.port=port;
        this.key=group+"-"+host+":"+port;
        this.bindConfigKeySet =new HashSet<>();
        this.active=true;
        this.lastActiveTime=System.currentTimeMillis();
        this.notifyNode =ServerNode.build("rpc", host, port,shareClientTemplate,null );
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
    public void addBindConfigKey(String configKey){
        bindConfigKeySet.add(configKey);
        ConfigEvent configEvent=new ConfigEvent();
        String configValue= MonitorManager.getInstance().getConfig(configKey);
        configEvent.setConfigKey(configKey);
        configEvent.setConfigValue(configValue);
        notifyEvent(configEvent);
    }
    public String getKey() {
        return group+"-"+host+":"+port;
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

    }
    public static MonitorNode build(ISession session,String group,String host,int port) throws Exception{
        MonitorNode node=new MonitorNode(session,group,host,port);
        session.setAttribute(RPC_NODE_KEY,node.getKey());
        return node;
    }
    public void notifyEvent(NodeEvent event){
        notifyNode.asyncCall(MonitorConstant.MonitorClientMethod.MONITOR_EVENT, null, event);
                log.info("Notify event to node:address={},event={}", key, event.getType());
    }

    public ServerNode getNotifyNode() {
        return notifyNode;
    }
}
