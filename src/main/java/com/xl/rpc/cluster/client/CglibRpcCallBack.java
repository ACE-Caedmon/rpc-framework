package com.xl.rpc.cluster.client;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.dispatch.method.AsyncRpcCallBack;
import com.xl.utils.ClassUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2015/7/20.
 */
public class CglibRpcCallBack implements MethodInterceptor {
    private RpcClientApi rpcClientApi=SimpleRpcClientApi.getInstance();
    private boolean sync=true;
    public CglibRpcCallBack(boolean sync){
        this.sync=sync;
    }
    public CglibRpcCallBack(AsyncRpcCallBack rpcCallBack){
        this.sync=false;
    }
    @Override
    public Object intercept(Object o, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        Class[] interfaces=o.getClass().getInterfaces();
        Class controlInterface=null;
        for(Class i:interfaces){
            if(ClassUtils.hasAnnotation(i,RpcControl.class)){
                controlInterface=i;
                break;
            }
        }
        if(controlInterface==null){
            throw new IllegalArgumentException("Interface has no @RpcControl annotation:class = "+o.getClass());
        }
        RpcControl rpcControl = (RpcControl) controlInterface.getAnnotation(RpcControl.class);
        String clusterName= rpcControl.value();
        RpcMethod cmdMethod=method.getAnnotation(RpcMethod.class);
        if(cmdMethod==null){
            return  null;
        }
        String cmd=method.getAnnotation(RpcMethod.class).value();
        Class returnType=method.getReturnType();
        Class[] paramTypes=method.getParameterTypes();
        for(int i=0;i<paramTypes.length;i++){
            paramTypes[i]=ClassUtils.getPackingType(paramTypes[i]);
        }
        if(sync){
            return rpcClientApi.syncRpcCall(clusterName,cmd,returnType,paramTypes,params);
        }else{
            rpcClientApi.asyncRpcCall(clusterName,cmd,paramTypes,params);
            return null;
        }
    }
}
