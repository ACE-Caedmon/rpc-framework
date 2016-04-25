package com.xiaoluo.rpc.registry.client;

import com.xiaoluo.rpc.annotation.RpcService;
import com.xiaoluo.rpc.annotation.RpcMethod;
import com.xiaoluo.rpc.registry.RegistryConstant;
import com.xiaoluo.rpc.registry.event.RegistryEvent;

/**
 * Created by Caedmon on 2015/9/17.
 */
@RpcService(RegistryConstant.MONITOR_CLIENT_SERVER)
public interface IRpcRegistryClientService {
    @RpcMethod(RegistryConstant.MonitorClientMethod.MONITOR_EVENT)
    void handleNodeEvent(RegistryEvent event);

}
