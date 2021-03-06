package com.xiaoluo.rpc.cluster.client;

import com.xiaoluo.rpc.exception.ClusterException;
import com.xiaoluo.utils.MurmurHash;
import redis.clients.util.SafeEncoder;

import java.util.*;

/**
 * Created by Caedmon on 2015/7/22.
 * 集群组
 */
public class ClusterGroup{
    private String clusterName;
    private Map<String,ServerNode> resources;
    private int callCount;
    private TreeMap<Long, ServerNode> hashNodes=new TreeMap<>();
    private final MurmurHash algo=new MurmurHash();
    public final static int VIRTUAL_NUM = 160;
    public ClusterGroup(String clusterName){
        this.clusterName=clusterName;
        this.resources =new LinkedHashMap<>();
    }

    public String getClusterName() {
        return clusterName;
    }

    public Collection<ServerNode> getResources() {
        return resources.values();
    }
    private ServerNode getHashNode(long hash){
        Long key = hash;
        SortedMap<Long, ServerNode> tailMap= hashNodes.tailMap(key);
        if(tailMap.isEmpty()) {
            key = hashNodes.firstKey();
        } else {
            key = tailMap.firstKey();
        }
        return hashNodes.get(key);
    }
    public void removeNode(String severKey){
        ServerNode serverNode= resources.remove(severKey);
        if(serverNode!=null){
            for(int j=0; j<VIRTUAL_NUM*serverNode.getWeight(); ++j) {
                long hash=algo.hash(SafeEncoder.encode("SHARD-" + serverNode.getKey()+ "-NODE-" + j));
                hashNodes.remove(hash);
            }
            serverNode.destory();
        }
    }
    public void addNode(ServerNode serverNode){
        resources.put(serverNode.getKey(), serverNode);
        for(int j=0; j<VIRTUAL_NUM*serverNode.getWeight(); ++j) {
            long hash=algo.hash(SafeEncoder.encode("SHARD-" + serverNode.getKey()+ "-NODE-" + j));
            hashNodes.put(hash, serverNode);
        }
    }
    public ServerNode getShardNode(String key){
        long hash=algo.hash(SafeEncoder.encode(key));
        return getHashNode(hash);
    }
    public ServerNode getOptimalServerNode() {
        callCount++;
        List<ServerNode> nodes= new ArrayList<>(resources.values());
        if(resources.isEmpty()){
            throw new ClusterException("Group has active node:clusterName = "+clusterName);
        }
        return (ServerNode)nodes.toArray()[callCount%nodes.size()];
    }

    public List<ServerNode> getNodeList(){
        return new ArrayList<>(resources.values());
    }

}
