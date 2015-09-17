package com.xl.rpc.internal;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;

/**
 * Created by Caedmon on 2015/9/17.
 */
@RpcControl("rpc")
public interface IMonitorControl {
    @RpcMethod("rpc.monitor")
    MonitorInformation getMonitorInformation();
}
