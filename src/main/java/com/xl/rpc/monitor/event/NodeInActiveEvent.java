package com.xl.rpc.monitor.event;

/**
 * Created by Administrator on 2015/9/24.
 */
public class NodeInActiveEvent extends NodeEvent {
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
}
