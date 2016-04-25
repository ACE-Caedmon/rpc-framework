package com.xiaoluo.rpc.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/9/29.
 */
public class RegistryClusterGroup {
    private String key;
    private Map<String,RegistryNode> nodes=new ConcurrentHashMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, RegistryNode> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, RegistryNode> nodes) {
        this.nodes = nodes;
    }

    public void add(RegistryNode node){
        nodes.put(node.getKey(), node);
    }
    public RegistryNode delete(String key){
        return nodes.remove(key);
    }
}
