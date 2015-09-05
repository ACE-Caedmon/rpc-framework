package com.xl.rpc.cluster.server;

import com.xl.rpc.dispatch.method.BeanAccess;

/**
 * Created by Administrator on 2015/7/15.
 */
public interface RpcServerApi {
    void bind();
    String getHost();
    int getPort();
    String getSelfClusterName();
    String getZKServerAddr();
    boolean isStarted();
    BeanAccess getBeanAccess();
}
