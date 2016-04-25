package com.xiaoluo.rpc.registry.event;

import java.util.Properties;

/**
 * Created by Caedmon on 2015/11/12.
 */
public class RouteEvent extends RegistryEvent {
    private Properties routeTable=new Properties();

    public RouteEvent() {
        this.type=NodeEventType.ROUTE_UPDATED;
    }

    public Properties getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(Properties routeTable) {
        this.routeTable = routeTable;
    }
}
