package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.codec.RpcPacket;
import com.xiaoluo.rpc.dispatch.ISession;

/**
 * Created by Caedmon on 2015/7/11.
 */
public class NoOpMethodExecutor implements MethodExecutor<ISession> {
    @Override
    public void execute(ISession session, RpcPacket packet) {
        //自动解码
        //调用control对应方法
        //将返回值发送回去
    }

    @Override
    public ISession getRpcSession(ISession session) {
        return session;
    }
}
