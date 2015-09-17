package com.xl.rpc.internal;

import com.xl.utils.SysPropertyConfig;

/**
 * Created by Administrator on 2015/9/17.
 */
public class MonitorControl implements IMonitorControl {
    @Override
    public MonitorInformation getMonitorInformation() {
        MonitorInformation information=new MonitorInformation();
        information.setClusterName(SysPropertyConfig.get("rpc.server.clusterNames"));
        information.setPort(SysPropertyConfig.getInt("rpc.server.port", 0));
        return information;
    }
}
