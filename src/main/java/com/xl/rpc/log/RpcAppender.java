package com.xl.rpc.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * Created by Administrator on 2015/8/27.
 */
public class RpcAppender extends AbstractSocketAppender<ILoggingEvent>{
    private  boolean includeCallerData;
    static {
        PatternLayout.defaultConverterMap.put("serviceName",RpcLogConverter.ServiceNameConvert.class.getName());
        PatternLayout.defaultConverterMap.put("address", RpcLogConverter.AddressConvert.class.getName());
    }
    private RpcSerializationTransformer transformer=new RpcSerializationTransformer();

    @Override
    protected void postProcessEvent(ILoggingEvent event) {
        if(this.includeCallerData) {
            event.getCallerData();
        }
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    @Override
    protected PreSerializationTransformer getPST() {
        return transformer;
    }
}
