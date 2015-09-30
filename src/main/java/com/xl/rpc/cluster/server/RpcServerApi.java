package com.xl.rpc.cluster.server;

import com.xl.rpc.dispatch.RpcMethodInterceptor;
import com.xl.rpc.dispatch.method.BeanAccess;

import java.util.List;

/**
 * Created by Administrator on 2015/7/15.
 */
public interface RpcServerApi {
    void bind();
    String getHost();
    int getPort();
    List<String> getClusterNames();
    String getMonitorServerAddress();
    boolean isStarted();
    BeanAccess getBeanAccess();
    void addInterceptor(RpcMethodInterceptor interceptor);
}
