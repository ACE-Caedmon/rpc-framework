package com.xl.rpc.monitor.event;

/**
 * Created by Administrator on 2015/9/24.
 */
public class ConfigEvent extends NodeEvent {
    private String configKey;
    private String configValue;

    public ConfigEvent() {
        this.type=NodeEventType.CONFIG_UPDATED;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

}
