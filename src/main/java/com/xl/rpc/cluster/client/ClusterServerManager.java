package com.xl.rpc.cluster.client;

import com.xl.rpc.monitor.MonitorNode;
import com.xl.rpc.monitor.client.RpcMonitorClient;
import com.xl.rpc.dispatch.RpcMethodInterceptor;
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
    private RpcMonitorClient rpcMonitorClient;
    private int callCount;
    private RpcClientTemplate rpcClientTemplate;
    private String centerAddress;
    private static final Logger log= LoggerFactory.getLogger(ClusterServerManager.class);
    private List<RpcMethodInterceptor> interceptors;
    public ClusterServerManager(String centerAddress,RpcClientTemplate rpcClientTemplate,List<RpcMethodInterceptor> interceptors){
        this.centerAddress=centerAddress;
        this.rpcClientTemplate=rpcClientTemplate;
        rpcMonitorClient = RpcMonitorClient.getInstance();
        try{
            rpcMonitorClient.connect(centerAddress);
            Map<String,MonitorNode> allNodes= rpcMonitorClient.getAllNodeMap();
            for(MonitorNode n:allNodes.values()){
                ClusterGroup group=getGroupByName(n.getGroup());
                if(group==null){
                    addClusterGroup(n.getGroup());
                }
                if(n.isActive()){
                    addServerNode(n.getGroup(), n.getHost(), n.getPort());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Register to center error",e);
            return;
        }

        this.interceptors=interceptors;
    }
    public void addServerNode(String group,String host,int port){
        ServerNode serverNode=null;
        try{
            serverNode=ServerNode.build(group, host, port, this.rpcClientTemplate, interceptors);
            log.error("Add server node success:address={}",serverNode.toString());
        }catch (Exception e){
            e.printStackTrace();
            log.error("Add server node error:address={}-{}:{}",group,host,port,e);
        }
        addNode(serverNode);
    }
    @Override
    public ServerNode getOptimalServerNode(String clusterName) {
        callCount++;
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
        log.debug("Add server:address={}",node.getKey());
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
            group.removeNode(key);
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
        log.info("Add group:{}",groupName);
        return group;
    }
}
