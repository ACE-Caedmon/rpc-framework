package com.xiaoluo.utils;

import io.netty.util.internal.SystemPropertyUtil;

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
