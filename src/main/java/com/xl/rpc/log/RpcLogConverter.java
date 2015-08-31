package com.xl.rpc.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Created by Administrator on 2015/8/27.
 */
public class RpcLogConverter {
    public static class ServiceNameConvert extends ClassicConverter{
        @Override
        public String convert(ILoggingEvent event) {
            if(event instanceof RpcLogEventVO){
                RpcLogEventVO rpcLogEventVO=(RpcLogEventVO)event;
                return rpcLogEventVO.getServiceName();
            }
            return "self";
        }
    }
    public static class AddressConvert extends ClassicConverter{
        @Override
        public String convert(ILoggingEvent event) {
            if(event instanceof RpcLogEventVO){
                RpcLogEventVO rpcLogEventVO=(RpcLogEventVO)event;
                return rpcLogEventVO.getAddress();
            }
            return "local";
        }
    }
}
