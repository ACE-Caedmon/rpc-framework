package com.xl.rpc.monitor.event;

/**
 * Created by Administrator on 2015/9/24.
 */
public class NodeActiveEvent extends NodeEvent{
    private String nodeKey;
    private String group;
    private String host;
    private int port;
    public NodeActiveEvent() {
        this.type=NodeEventType.NODE_ACTIVE;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeKey() {
        return nodeKey;
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
}
