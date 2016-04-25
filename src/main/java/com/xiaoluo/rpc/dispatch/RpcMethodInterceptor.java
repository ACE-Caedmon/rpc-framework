package com.xiaoluo.rpc.dispatch;

import com.xiaoluo.rpc.codec.RpcPacket;

/**
 * Created by Caedmon on 2015/7/13.
 */
public interface RpcMethodInterceptor {
    boolean beforeExecuteCmd(ISession session, RpcPacket packet);
    void afterExecuteCmd(ISession session, RpcPacket packet);
    void exceptionCaught(ISession session, RpcPacket packet, Throwable cause);
}
