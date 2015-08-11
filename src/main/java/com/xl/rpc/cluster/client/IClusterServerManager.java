package com.xl.rpc.cluster.client;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/14.
 */
public interface IClusterServerManager {
    /**
     * 获取最优服务器节点
     * */
    ServerNode getOptimalServerNode(String clusterName);
    /**
     * 添加服务器节点
     * */
    void addServerNode(ServerNode node);
    /**
     * 添加服务器节点
     * */
    void addServerNode(List<ServerNode> nodeList);
    /**
     * 刷新服务器节点状态
     * */
    void updateServerNode(ServerNode serverNode);
    /**
     * 根据集群名获取节点列表
     * */
    ClusterGroup getGroupByName(String clusterName);
    /**
     * 移除节点
     * */
    void removeServerNode(String key);
    /**
     * 根据Id获取服务器节点
     * */
    ServerNode getServerNode(String key);

    Map<String,ServerNode> getAllServerNodes();

    ClusterGroup addClusterGroup(String clusterName);
}
