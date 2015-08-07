package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.session.ISession;

/**
 * Created by Caedmon on 2015/7/11.
 */
public abstract class ControlMethod<T> {
    protected RpcPacket packet;
    public ControlMethod(RpcPacket packet){
        this.packet=packet;
    }
    public  abstract void doCmd(ISession session);
    public abstract T getCmdUser(ISession session);

    public RpcPacket getPacket() {
        return packet;
    }

}
