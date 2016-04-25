package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.codec.RpcPacket;
import com.xiaoluo.rpc.dispatch.ISession;

/**
 * Created by Caedmon on 2015/7/11.
 */
public interface MethodExecutor<T> {
    void execute(ISession session, RpcPacket packet);
    T getRpcSession(ISession session);

}
