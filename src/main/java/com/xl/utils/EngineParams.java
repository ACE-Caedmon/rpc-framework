package com.xl.utils;

import com.xl.rpc.annotation.MsgType;
import io.netty.util.internal.SystemPropertyUtil;
import org.springframework.util.SystemPropertyUtils;

/**
 * Created by Administrator on 2015/5/7.
 */
public class EngineParams {
    /**是否开启Netty的LoggingHandler*/
    public static boolean isNettyLogging(){
        return SystemPropertyUtil.getBoolean("ng.socket.netty.loggging",false);
    }
    public static final boolean isWriteJavassit(){
        return SystemPropertyUtil.getBoolean("javassit.writeClass",false);
    }
}
