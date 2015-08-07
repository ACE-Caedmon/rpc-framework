package com.xl.rpc.dispatch;

import com.xl.rpc.codec.RpcPacket;
import com.xl.session.ISession;

/**
 * Created by Caedmon on 2015/7/13.
 */
public interface CmdInterceptor {
    boolean beforeDoCmd(ISession session,RpcPacket packet);
    void afterDoCmd(ISession session,RpcPacket packet);
    void exceptionCaught(ISession session,RpcPacket packet,Throwable cause);
}
