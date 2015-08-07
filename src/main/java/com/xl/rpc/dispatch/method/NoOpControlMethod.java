package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.session.ISession;

/**
 * Created by Caedmon on 2015/7/11.
 */
public class NoOpControlMethod extends ControlMethod<ISession> {
    public NoOpControlMethod(RpcPacket packet) {
        super(packet);
    }
    @Override
    public void doCmd(ISession session) {
        //自动解码
        //调用control对应方法
        //将返回值发送回去
        //包未处理过，并且是同步消息，则需要响应
    }

    @Override
    public ISession getCmdUser(ISession session) {
        return session;
    }
}
