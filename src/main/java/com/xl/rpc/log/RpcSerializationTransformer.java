package com.xl.rpc.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.spi.PreSerializationTransformer;

import java.io.Serializable;

/**
 * Created by Administrator on 2015/8/27.
 */
public class RpcSerializationTransformer implements PreSerializationTransformer<ILoggingEvent> {
    @Override
    public Serializable transform(ILoggingEvent event) {
        if(event==null){
            return null;
        }
        if(event instanceof LoggingEvent){
            RpcLogEventVO rpcLogEventVO = RpcLogEventVO.build(event);
            return rpcLogEventVO.build(event);
        } else if (event instanceof RpcLogEventVO) {
            return (RpcLogEventVO)  event;
        } else {
            throw new IllegalArgumentException("Unsupported type "+event.getClass().getName());
        }
    }
}
