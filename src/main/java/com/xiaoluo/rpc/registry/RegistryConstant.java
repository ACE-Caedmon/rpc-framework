package com.xiaoluo.rpc.registry;

/**
 * Created by Administrator on 2015/9/25.
 */
public class RegistryConstant {
    public static final String MONITOR_CLIENT_SERVER ="monitorClient";
    public static final String MONITOR_SERVER_NAME="monitorServer";
    public static class MonitorClientMethod{
        public static final String MONITOR_EVENT="monitorClient.event";
        public static final String SHUT_DOWN="monitorClient.shutdown";
    }
    public static final class MonitorServerMethod{
        public static final String GET_ALL_NODE_MAP="monitorServer.getAllNodeMap";
        public static final String REGISTER="monitorServer.register";
        public static final String REGISTER_WITH_HOST ="monitorServer.registerWithHost";
        public static final String DELETE="monitorServer.deleteNode";
        public static final String UPDATE_CONFIG="monitorServer.updateConfig";
        public static final String GET_CONFIG="monitorServer.getConfig";
        public static final String HEART_BEAT="monitorServer.heartbeat";
        public static final String BIND_CONFIG="monitorServer.bindNodeConfig";
    }
}
