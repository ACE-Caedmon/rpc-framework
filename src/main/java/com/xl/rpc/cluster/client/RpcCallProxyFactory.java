package com.xl.rpc.cluster.client;

import com.xl.rpc.dispatch.method.AsyncRpcCallBack;

/**
 * Created by Administrator on 2015/7/20.
 */
public interface RpcCallProxyFactory {
    class CallProxyEntry<T>{
        T syncProxy;
        T asyncProxy;
    }
    <T> T getRpcCallProxy(boolean sync, Class<T> clazz);
    <T> CallProxyEntry<T> createCallProxyEntry(Class<T> clazz);
}
