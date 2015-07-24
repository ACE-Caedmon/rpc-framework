package com.xl.utils;

import com.xl.annotation.MsgType;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * Created by Administrator on 2015/5/7.
 */
public class NGSocketParams {
    /**是否开启Netty的LoggingHandler*/
    public static boolean isNettyLogging(){
        return SystemPropertyUtil.getBoolean("ng.socket.netty.loggging",false);
    }
    /**出现未知Cmd时，是否日志提醒*/
    public static final boolean isWarnUnKownCmd(){
        return SystemPropertyUtil.getBoolean("ng.socket.warn.unkowncmd",true);
    }
    /**是否加密数据包*/
    public static final boolean isSocketPacketEncrypt(){
        return SystemPropertyUtil.getBoolean("ng.socket.packet.encrypt", false);
    }
    /**数据包加密解密的密钥*/
    public static final String getSocketSecretKey(){
        return SystemPropertyUtil.get("ng.socket.secret.key","NG-Socket");
    }
    public static final MsgType getSystemMsgType(){
        return MsgType.valueOf(SystemPropertyUtil.get("ng.socket.msg.type", MsgType.JSON.name()));
    }
}
