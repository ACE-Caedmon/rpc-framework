package com.xl.rpc.cluster.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/7/14.
 */
public class ClusterServerManager implements IClusterServerManager {
    private Map<String,ClusterGroup> clusterGroupMap =new ConcurrentHashMap<>();
    private Map<String,ServerNode> allServersMap =new ConcurrentHashMap<>();
    private static ClusterServerManager instance=new ClusterServerManager();
    private int callCount;
    private static final Logger log= LoggerFactory.getLogger(ClusterServerManager.class);
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
            throw new IllegalArgumentException("Cluster not exists  '"+clusterName+"'");
        }
        return clusterGroup.getOptimalServerNode();
    }

    @Override
    public void addServerNode(ServerNode node) {
        allServersMap.put(node.getKey(), node);
        ClusterGroup group=clusterGroupMap.get(node.getClusterName());
        if(group==null){
            group=new ClusterGroup(node.getClusterName());
        }
        group.addNode(node);
        log.debug("Add server node:{}",node.getKey());
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
        ServerNode node= allServersMap.put(serverKey, serverNode);
    }

    @Override
    public ClusterGroup getGroupByName(String clusterName) {
        ClusterGroup group=clusterGroupMap.get(clusterName);
        return group;
    }

    @Override
    public void removeServerNode(String key) {
        ServerNode result= allServersMap.remove(key);
        if(result!=null){
            ClusterGroup group=clusterGroupMap.get(result.getClusterName());
            group.removeNode(key);
        }
    }

    @Override
    public ServerNode getServerNode(String key) {
        return allServersMap.get(key);
    }

    @Override
    public Map<String, ServerNode> getAllServerNodes() {
        return allServersMap;
    }

    @Override
    public void addClusterGroup(String clusterName) {
        clusterGroupMap.put(clusterName,new ClusterGroup(clusterName));
    }
}
