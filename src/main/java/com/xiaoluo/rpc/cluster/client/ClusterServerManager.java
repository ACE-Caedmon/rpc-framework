package com.xiaoluo.rpc.cluster.client;

import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.registry.client.SimpleRegistryApi;
import com.xiaoluo.rpc.dispatch.RpcMethodInterceptor;
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
    private SimpleRegistryApi simpleRegistryApi;
    private RpcClientTemplate rpcClientTemplate;
    private static final Logger log= LoggerFactory.getLogger(ClusterServerManager.class);
    private List<RpcMethodInterceptor> interceptors;
    public ClusterServerManager(RpcClientTemplate rpcClientTemplate,List<RpcMethodInterceptor> interceptors){
        this.rpcClientTemplate=rpcClientTemplate;
        this.interceptors=interceptors;
        try {
            loadAllRpcNodes();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Load all rpc nodes error",e);
        }
    }
    public void loadAllRpcNodes() throws Exception{
        this.simpleRegistryApi = SimpleRegistryApi.getInstance();
        if(!this.simpleRegistryApi.isCompleted()){
            throw new IllegalStateException("You must start SimpleRegistryApi first,otherwise you will not get the server node information");
        }
        Map<String,RegistryNode> allNodes= simpleRegistryApi.getAllNodeMap();
        log.info("Load rpc nodes from registry server:{}",allNodes);
        for(RegistryNode n:allNodes.values()){
            ClusterGroup group=getGroupByName(n.getGroup());
            if(group==null){
                addClusterGroup(n.getGroup());
            }
            if(n.isActive()){
                addServerNode(n.getGroup(), n.getHost(), n.getPort());
            }
        }
    }
    public void addServerNode(String group,String host,int port){
        ServerNode serverNode=null;
        try{
            serverNode=ServerNode.build(group, host, port, this.rpcClientTemplate, interceptors);
        }catch (Exception e){
            e.printStackTrace();
            log.error("Server manager create node error {}-{}:{}",group,host,port,e);
        }
        addNode(serverNode);
    }
    @Override
    public ServerNode getOptimalServerNode(String clusterName) {
        ClusterGroup clusterGroup=clusterGroupMap.get(clusterName);
        if(clusterGroup==null){
            clusterGroup=addClusterGroup(clusterName);
        }
        return clusterGroup.getOptimalServerNode();
    }

    @Override
    public void addNode(ServerNode node) {
        deleteNode(node.getKey());
        allServersMap.put(node.getKey(), node);
        ClusterGroup group=clusterGroupMap.get(node.getClusterName());
        if(group==null){
            group=new ClusterGroup(node.getClusterName());
        }
        group.addNode(node);
        log.debug("Server manager add node {}",node.getKey());
    }

    @Override
    public void addNode(List<ServerNode> nodeList) {
        for(ServerNode node:nodeList){
            addNode(node);
        }
    }

    @Override
    public void updateNode(ServerNode serverNode) {
        String serverKey=serverNode.getKey();
        ServerNode node= allServersMap.put(serverKey, serverNode);
    }

    @Override
    public ClusterGroup getGroupByName(String clusterName) {
        ClusterGroup group=clusterGroupMap.get(clusterName);
        return group;
    }

    @Override
    public void deleteNode(String key) {
        ServerNode result= allServersMap.remove(key);
        if(result!=null){
            ClusterGroup group=clusterGroupMap.get(result.getClusterName());
            if(group==null){
                return;
            }
            group.removeNode(key);
            log.debug("Server manager delete node {}",key);
        }
    }

    @Override
    public ServerNode getServerNode(String key) {
        String clusterName=key.split("-")[0];
        ClusterGroup clusterGroup=clusterGroupMap.get(clusterName);
        if(clusterGroup==null){
            addClusterGroup(clusterName);
        }
        ServerNode node=allServersMap.get(key);
        return node;
    }

    @Override
    public Map<String, ServerNode> getAllServerNodes() {
        return allServersMap;
    }

    @Override
    public ClusterGroup addClusterGroup(String groupName) {
        ClusterGroup group=new ClusterGroup(groupName);
        clusterGroupMap.put(groupName,group);
        log.info("Server manager add  group '{}'",groupName);
        return group;
    }
}
