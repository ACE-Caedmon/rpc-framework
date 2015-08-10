package com.xl.rpc.cluster.server;

/**
 * Created by Administrator on 2015/7/15.
 */
public interface RpcServerApi {
    void bind();
    String getHost();
    int getPort();
    String getSelfClusterName();
    String getZKServerAddr();
}
