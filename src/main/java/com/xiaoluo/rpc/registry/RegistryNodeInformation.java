package com.xiaoluo.rpc.registry;

import com.xiaoluo.rpc.dispatch.RpcCallInfo;

/**
 * Created by Caedmon on 2015/9/17.
 */
public class RegistryNodeInformation {
    private RpcCallInfo rpcCallInfo;

    public RpcCallInfo getRpcCallInfo() {
        return rpcCallInfo;
    }

    public void setRpcCallInfo(RpcCallInfo rpcCallInfo) {
        this.rpcCallInfo = rpcCallInfo;
    }
}
