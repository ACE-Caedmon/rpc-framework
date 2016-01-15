package com.xl.rpc.monitor.server;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.monitor.MonitorConstant;
import com.xl.rpc.monitor.MonitorInformation;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.session.ISession;

import java.util.Map;

/**
 * Created by Administrator on 2015/9/22.
 */
@RpcControl(MonitorConstant.MONITOR_SERVER_NAME)
public interface IRpcMonitorServerService {
    @RpcMethod(MonitorConstant.MonitorServerMethod.GET_ALL_NODE_MAP)
    Map<String,MonitorNode> getAllNodeMap();
    @RpcMethod(MonitorConstant.MonitorServerMethod.REGISTER_WITH_HOST)
    MonitorNode registerWithHost(ISession session, String[] groups, String host, int port) throws Exception;
    @RpcMethod(MonitorConstant.MonitorServerMethod.REGISTER)
    MonitorNode register(ISession session, String[] groups, int port) throws Exception;
    @RpcMethod(MonitorConstant.MonitorServerMethod.DELETE)
    void deleteNode(String key);
    @RpcMethod(MonitorConstant.MonitorServerMethod.UPDATE_CONFIG)
    void updateConfig(String key, String config);
    @RpcMethod(MonitorConstant.MonitorServerMethod.GET_CONFIG)
    String getConfig(String key);
    @RpcMethod(MonitorConstant.MonitorServerMethod.HEART_BEAT)
    String heartBeat(ISession session, MonitorInformation information) throws Exception;
    @RpcMethod(MonitorConstant.MonitorServerMethod.BIND_CONFIG)
    String bindNodeConfig(String nodeKey, String configKey);

}
