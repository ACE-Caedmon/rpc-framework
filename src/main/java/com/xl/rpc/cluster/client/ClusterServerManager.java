package com.xl.rpc.cluster.client;

import com.xl.rpc.boot.RpcClientSocketEngine;
import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.cluster.ZkServiceDiscovery;
import com.xl.rpc.dispatch.method.BeanAccess;
import com.xl.rpc.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.exception.ClusterNotExistsException;
import com.xl.rpc.exception.EngineException;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/7/14.
 */
public class ClusterServerManager implements IClusterServerManager {
    private Map<String,ClusterGroup> clusterGroupMap =new ConcurrentHashMap<>();
    private Map<String,ServerNode> allServersMap =new ConcurrentHashMap<>();
    private ZkServiceDiscovery zkServiceDiscovery;
    private int callCount;
    private RpcClientTemplate rpcClientTemplate;
    private static final Logger log= LoggerFactory.getLogger(ClusterServerManager.class);
    public ClusterServerManager(RpcClientTemplate rpcClientTemplate){
        this.rpcClientTemplate=rpcClientTemplate;
        zkServiceDiscovery =new ZkServiceDiscovery(rpcClientTemplate.getZookeeperAddress());
        zkServiceDiscovery.setListener(new ZkServiceDiscovery.ServerDiscoveryListener() {
            @Override
            public void onServerListChanged(String s) {
                refreshClusterServers(s);
            }
        });
        for(String clusterName: zkServiceDiscovery.getAllServerMap().keySet()){
            ClusterGroup group=getGroupByName(clusterName);
            if(group==null){
                addClusterGroup(clusterName);
            }
        }
    }
    public void refreshClusterServers(String clusterName){
        List<String> newServerAddressList= zkServiceDiscovery.getServerList(clusterName);
        ClusterGroup group=getGroupByName(clusterName);
        if(group==null){
            log.debug("Skip refresh cluster:{}",clusterName);
            return;
        }
        Map<String,ServerNode> oldServers=getAllServerNodes();
        //遍历新数据
        for(String address:newServerAddressList){
            String[] hostAndPort=address.split(":");
            String remoteHost=hostAndPort[0];
            int remotePort=Integer.parseInt(hostAndPort[1]);
            //如果节点存在于新数据,而老数据中没有，则是一个新的节点，需要新建连接
            if(!oldServers.containsKey(clusterName+"-"+address)){
                try{
                    ServerNode serverNode=newServerNode(clusterName,remoteHost, remotePort);
                    serverNode.setClusterName(clusterName);
                    addServerNode(serverNode);
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("Update server node error: {}",address,e);
                }
                continue;
            }
        }

        List<ServerNode> oldServerNodes=group.getNodeList();
        if(oldServerNodes!=null){
            Iterator<ServerNode> it=oldServerNodes.iterator();
            while(it.hasNext()){
                ServerNode oldNode=it.next();
                //如果节点存在于旧数据,而新数据中没有，则节点需要移除，需要销毁
                boolean needRemove=true;
                for(String address:newServerAddressList){
                    if(oldNode.getKey().equals(clusterName+"-"+address)){
                        needRemove=false;
                        break;
                    }
                }
                if(needRemove){
                    it.remove();
                    removeServerNode(oldNode.getKey());
                    log.info("Remove server node :server = {}",oldNode.getKey());
                }
            }
        }

    }
    public ServerNode newServerNode(String clusterName,String remoteHost,int remotePort) throws Exception{
        TCPClientSettings settings=this.rpcClientTemplate.createClientSettings(remoteHost, remotePort);
        BeanAccess beanAccess=null;
        try{
            beanAccess=(BeanAccess)Class.forName(rpcClientTemplate.getBeanAccessClass()).newInstance();
        }catch (Exception e){
            throw new EngineException("BeanAccess init error",e);
        }
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(beanAccess,rpcClientTemplate.getCmdThreadSize());
        RpcClientSocketEngine clientSocketEngine=new RpcClientSocketEngine(settings,dispatcher,rpcClientTemplate.getLoopGroup());
        clientSocketEngine.start();
        ServerNode serverNode=new ServerNode(clientSocketEngine);
        serverNode.setHost(remoteHost);
        serverNode.setPort(remotePort);
        serverNode.setSyncCallTimeout(settings.syncTimeout);
        serverNode.setClusterName(clusterName);
        log.info("Create new server:{}", serverNode.getKey());
        return serverNode;
    }
    @Override
    public ServerNode getOptimalServerNode(String clusterName) {
        callCount++;
        ClusterGroup clusterGroup=clusterGroupMap.get(clusterName);
        if(clusterGroup==null){
            throw new ClusterNotExistsException("Cluster not exists  '"+clusterName+"'");
        }
        if(clusterGroup.getNodeList().isEmpty()){
            refreshClusterServers(clusterName);
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
    public ClusterGroup addClusterGroup(String clusterName) {
        ClusterGroup group=new ClusterGroup(clusterName);
        clusterGroupMap.put(clusterName,group);
        log.info("Add cluster group:{}",clusterName);
        return group;
    }
}
