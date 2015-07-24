package com.xl.cluster.client;

import net.sf.cglib.proxy.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/7/20.
 */
public class CglibRpcCallProxyFactory implements RpcCallProxyFactory{
    private static final MethodInterceptor COMMON_CGLIB_CALLBACK=new CglibRpcCallBack();
    private Map<Class,Object> proxyCache=new ConcurrentHashMap<>();
    @Override
    public <T> T getRpcCallProxy(Class<T> clazz) {
        T proxy=(T)proxyCache.get(clazz);
        if(proxy==null){
            createRpcCallProxy(clazz);
            proxyCache.put(clazz,proxy);
        }
        return proxy;
    }

    @Override
    public <T> T createRpcCallProxy(Class<T> clazz) {
        Enhancer enhancer = new Enhancer();//通过类Enhancer创建代理对象
        enhancer.setSuperclass(clazz);//传入创建代理对象的类
        enhancer.setCallback(COMMON_CGLIB_CALLBACK);//设置回调
        T proxy=(T)enhancer.create();//创建代理对象
        return proxy;
    }
}
