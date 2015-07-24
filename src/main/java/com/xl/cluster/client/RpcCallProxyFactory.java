package com.xl.cluster.client;

/**
 * Created by Administrator on 2015/7/20.
 */
public interface RpcCallProxyFactory {
    <T> T getRpcCallProxy(Class<T> clazz);
    <T> T createRpcCallProxy(Class<T> clazz);
}
