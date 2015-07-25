package com.xl.dispatch.method;

import com.xl.codec.RpcPacket;
import com.xl.session.ISession;
import com.xl.session.Session;

/**
 * Created by Caedmon on 2015/7/14.
 */
public class SyncControlMethod extends NoOpControlMethod {
    public SyncControlMethod(RpcPacket packet) {
        super(packet);
    }

    @Override
    public void doCmd(ISession session) {
        //根据消息ID查找Future
        SyncRpcCallBack callBack=session.getAttribute(Session.SYNC_CALLBACK_MAP).get(packet.getUuid());
        if(callBack!=null){
            if(packet.isException()){
                callBack.processException((Throwable) packet.getParams()[0]);
                return;
            }
            if(packet.getParams().length==1&&packet.getClassNameArray().length==1){
                callBack.processResult(session,packet.getParams()[0]);
            }else{
                callBack.processResult(session,packet.getParams());
            }

            return;
        }
    }
}
