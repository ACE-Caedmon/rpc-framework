package com.xl.rpc.monitor.client;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.monitor.MonitorConstant;
import com.xl.rpc.monitor.event.MonitorEvent;

/**
 * Created by Caedmon on 2015/9/17.
 */
@RpcControl(MonitorConstant.MONITOR_CLIENT_SERVER)
public interface IRpcMonitorClientService {
    @RpcMethod(MonitorConstant.MonitorClientMethod.MONITOR_EVENT)
    void handleNodeEvent(MonitorEvent event);

}
