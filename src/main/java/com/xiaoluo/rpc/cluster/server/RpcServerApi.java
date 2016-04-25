package com.xiaoluo.rpc.cluster.server;

import com.xiaoluo.rpc.dispatch.RpcMethodInterceptor;
import com.xiaoluo.rpc.dispatch.method.BeanAccess;

import java.util.List;

/**
 * Created by Administrator on 2015/7/15.
 */
public interface RpcServerApi {
    void bind();
    int getPort();
    List<String> getClusterNames();
    String getMonitorServerAddress();
    boolean isStarted();
    BeanAccess getBeanAccess();
    void addInterceptor(RpcMethodInterceptor interceptor);
    void addScanPackage(String packageName);
}
