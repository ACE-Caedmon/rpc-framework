package com.xl.rpc.monitor.client;

import com.xl.rpc.monitor.event.MonitorEvent;

/**
 * Created by Administrator on 2015/10/12.
 */
public interface MonitorEventWatcher {
    void process(MonitorEvent event);
}
