package com.xl.rpc.monitor.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/9/24.
 */
public class ConfigEvent extends MonitorEvent {
    private Map<String,String> configMap=new HashMap<>();

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }
    public void addConfigEntity(String configKey,String configValue){
        this.configMap.put(configKey,configValue);
    }
    public ConfigEvent() {
        this.type=NodeEventType.CONFIG_UPDATED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigEvent{");
        sb.append("configMap=").append(configMap);
        sb.append('}');
        return sb.toString();
    }
}
