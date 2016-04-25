package com.xiaoluo.rpc.registry.server;

import com.xiaoluo.rpc.annotation.RpcService;
import com.xiaoluo.rpc.annotation.RpcMethod;
import com.xiaoluo.rpc.registry.RegistryConstant;
import com.xiaoluo.rpc.registry.RegistryNodeInformation;
import com.xiaoluo.rpc.registry.RegistryNode;
import com.xiaoluo.rpc.dispatch.ISession;

import java.util.Map;

/**
 * Created by Administrator on 2015/9/22.
 */
@RpcService(RegistryConstant.MONITOR_SERVER_NAME)
public interface IRpcRegistryServerService {
    @RpcMethod(RegistryConstant.MonitorServerMethod.GET_ALL_NODE_MAP)
    Map<String,RegistryNode> getAllNodeMap();
    @RpcMethod(RegistryConstant.MonitorServerMethod.REGISTER_WITH_HOST)
    RegistryNode registerWithHost(ISession session, String[] groups, String host, int port) throws Exception;
    @RpcMethod(RegistryConstant.MonitorServerMethod.REGISTER)
    RegistryNode register(ISession session, String[] groups, int port) throws Exception;
    @RpcMethod(RegistryConstant.MonitorServerMethod.DELETE)
    void deleteNode(String key);
    @RpcMethod(RegistryConstant.MonitorServerMethod.UPDATE_CONFIG)
    void updateConfig(String key, String config);
    @RpcMethod(RegistryConstant.MonitorServerMethod.GET_CONFIG)
    String getConfig(String key);
    @RpcMethod(RegistryConstant.MonitorServerMethod.HEART_BEAT)
    String heartBeat(ISession session, RegistryNodeInformation information) throws Exception;
    @RpcMethod(RegistryConstant.MonitorServerMethod.BIND_CONFIG)
    String bindNodeConfig(String nodeKey, String configKey);

}
