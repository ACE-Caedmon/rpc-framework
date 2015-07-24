package com.xl.cluster.client;

import com.xl.exception.ClusterException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/7/14.
 */
public class ClusterServerManager implements IClusterServerManager {
    private Map<String,ClusterGroup> clusterGroupMap =new ConcurrentHashMap<>();
    private Map<String,ServerNode> nodesByServerKey =new ConcurrentHashMap<>();
    private static ClusterServerManager instance=new ClusterServerManager();
    private int refreshInterval;
    private int callCount;
    private ClusterServerManager(){

    }
    public static ClusterServerManager getInstance(){
        return instance;
    }

    @Override
    public ServerNode getOptimalServerNode(String clusterName) {
        callCount++;
        ClusterGroup clusterGroup=clusterGroupMap.get(clusterName);
        if(clusterGroup==null){
            throw new IllegalArgumentException("集群不存在 clusterName = "+clusterName);
        }
        return clusterGroup.getOptimalServerNode();
    }

    @Override
    public void addServerNode(ServerNode node) {
        nodesByServerKey.put(node.getKey(), node);
        ClusterGroup group=clusterGroupMap.get(node.getClusterName());
        if(group==null){
            group=new ClusterGroup(node.getClusterName());
        }
        group.addNode(node);
    }

    @Override
    public void addServerNode(List<ServerNode> nodeList) {
        for(ServerNode node:nodeList){
            addServerNode(node);
        }
    }

    @Override
    public void updateServerNode(ServerNode serverNode) {
        String serverKey=serverNode.getKey();
        ServerNode node= nodesByServerKey.put(serverKey, serverNode);
    }

    @Override
    public ClusterGroup getGroupByName(String clusterName) {
        ClusterGroup group=clusterGroupMap.get(clusterName);
        return group;
    }

    @Override
    public void removeServerNode(String key) {
        ServerNode result= nodesByServerKey.remove(key);
        if(result!=null){
            ClusterGroup group=clusterGroupMap.get(result.getClusterName());
            group.removeNode(key);
        }
    }

    @Override
    public ServerNode getServerNode(String key) {
        return nodesByServerKey.get(key);
    }

    @Override
    public Map<String, ServerNode> getAllServerNodes() {
        return nodesByServerKey;
    }

    @Override
    public void addClusterGroup(String clusterName) {
        clusterGroupMap.put(clusterName,new ClusterGroup(clusterName));
    }
}
