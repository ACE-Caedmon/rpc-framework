package com.xl.rpc.cluster.client;

import com.xl.rpc.dispatch.CmdInterceptor;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by Caedmon on 2015/7/15.
 */
public interface RpcClientApi {
    /**
     * 初始化服务器节点信息，建立连接，保存映射关系
     * */
    void bind();
    /**
     * 异步发送消息到指定服务器集群
     * @param clusterName 集群名
     * @param cmd 消息命令
     * @param params 消息内容
     * */
    void asyncRpcCall(String clusterName, String cmd, Object... params);
    /**
     * 同步发送消息到指定服务器集群
     * @param clusterName 集群名
     * @param cmd 消息命令
     * @param params 消息内容
     * @return 返回的消息内容
     * */
    <T> T syncRpcCall(String clusterName, String cmd, Class<T> resultType, Object... params) throws TimeoutException;
    /**
     * 远程调用,此方式一定是同步调用的
     * */
    <T> T getSyncRemoteCallProxy(Class<T> clazz);

    <T> T getAsyncRemoteCallProxy(Class<T> clazz);
    /**
     * 根据组名获取到对应集群所有服务器列表
     * */
    List<ServerNode> getServersByClusterName(String clusterName);
    /**
     *@param clusterName 集群服务名
     *@param address 服务器节点唯一标识 localhost:8080
     *@param cmd 命令
     *@param resultType 返回的消息的Class
     *@params params 消息参数
     * */
    <T> T syncRpcCall(String clusterName,String address,String cmd,Class<T> resultType,Object...params) throws Exception;
    /**
     *@param clusterName 集群服务名
     *@param address 服务器节点唯一标识 localhost:8080
     *@param cmd 命令
     *@params params 消息参数
     * */
    void asyncRpcCall(String clusterName,String address,String cmd,Object...params) throws Exception;

    /**
     * 一致性hash选择节点发送消息，当节点存活时，同一个key绝对是发送给同一个服务器节点
     *@param clusterName 集群服务名
     *@param key 业务关键字
     *@param cmd 命令
     *@param resultType 返回的消息的Class
     *@params params 消息参数
     * */
    <T> T syncHashRpcCall(String clusterName,String key,String cmd,Class<T> resultType,Object...params) throws Exception;

    /**
     * 一致性Hash回调方式调用
     *@param clusterName 集群服务名
     *@param key 业务关键字
     *@param cmd 命令
     *@params params 消息参数
     * */
    void asyncHashRpcCall(String clusterName,String key,String cmd,AsyncRpcCallBack callBack,Object...params);
    /**
     *@param clusterName 集群服务名
     *@param key 业务关键字
     *@param cmd 命令
     *@params params 消息参数
     * */
    void asyncHashRpcCall(String clusterName,String key,String cmd,Object...params) throws Exception;

    void asyncRpcCall(String clusterName,String cmd,AsyncRpcCallBack callback,Object...params);

    void addRpcMethodInterceptor(CmdInterceptor interceptor);

}
