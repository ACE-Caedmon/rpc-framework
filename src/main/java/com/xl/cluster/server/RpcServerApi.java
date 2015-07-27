package com.xl.cluster.server;

import com.xl.cluster.client.ServerNode;

/**
 * Created by Administrator on 2015/7/15.
 */
public interface RpcServerApi {
    void bind();
    String getHost();
    int getPort();
}
