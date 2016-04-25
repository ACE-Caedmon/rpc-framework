package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.codec.RpcPacket;
import com.xiaoluo.rpc.exception.RemoteException;
import com.xiaoluo.rpc.dispatch.ISession;
import com.xiaoluo.rpc.dispatch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/7/14.
 */
public class SyncMethodExecutor extends NoOpMethodExecutor {
    private static final Logger log= LoggerFactory.getLogger(SyncMethodExecutor.class);
    @Override
    public void execute(ISession session, RpcPacket packet) {
        //log.debug("Sync method execute:cmd = {}",packet.getCmd());
        //根据消息ID查找Future
        SyncRpcCallBack callBack=session.getAttribute(Session.SYNC_CALLBACK_MAP).get(packet.getUuid());
        if(callBack!=null){
            if(packet.isException()){
                callBack.processException(new RemoteException((Throwable) packet.getParams()[0]));
                return;
            }
            if(packet.getParams().length==1&&packet.getClassNameArray().length==1){
                callBack.processResult(session,packet.getParams()[0]);
            }else{
                callBack.processResult(session, packet.getParams());

            }
            return;
        }
    }
}
