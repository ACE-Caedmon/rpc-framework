package com.xiaoluo.rpc.registry.client;

import com.xiaoluo.rpc.registry.event.RegistryEvent;

/**
 * Created by Administrator on 2015/10/12.
 */
public interface RegistryEventWatcher {
    void process(RegistryEvent event);
}
