package com.xl.rpc.monitor;

import com.xl.rpc.dispatch.RpcCallInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Caedmon on 2015/9/17.
 */
public class MonitorInformation {
    private RpcCallInfo rpcCallInfo;

    public RpcCallInfo getRpcCallInfo() {
        return rpcCallInfo;
    }

    public void setRpcCallInfo(RpcCallInfo rpcCallInfo) {
        this.rpcCallInfo = rpcCallInfo;
    }
}
