package com.xl.rpc.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/9/29.
 */
public class MonitorGroup {
    private String key;
    private Map<String,MonitorNode> nodes=new ConcurrentHashMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, MonitorNode> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, MonitorNode> nodes) {
        this.nodes = nodes;
    }

    public void add(MonitorNode node){
        nodes.put(node.getKey(), node);
    }
    public MonitorNode delete(String key){
        return nodes.remove(key);
    }
}
