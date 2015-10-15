package com.xl.rpc.monitor.event;

/**
 * Created by Administrator on 2015/9/24.
 */
public class NodeInActiveEvent extends MonitorEvent {
    private String nodeKey;
    public NodeInActiveEvent() {
        this.type=NodeEventType.NODE_INACTIVE;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeInActiveEvent{");
        sb.append("nodeKey='").append(nodeKey).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
