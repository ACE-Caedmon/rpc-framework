package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.session.ISession;

/**
 * Created by Caedmon on 2015/7/11.
 */
public interface MethodExecutor<T> {
    void execute(ISession session, RpcPacket packet);
    T getRpcSession(ISession session);

}
