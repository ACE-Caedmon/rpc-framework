package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.exception.RemoteException;
import com.xl.session.ISession;
import com.xl.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/7/14.
 */
public class SyncControlMethod extends NoOpControlMethod {
    private static final Logger log= LoggerFactory.getLogger(SyncControlMethod.class);
    public SyncControlMethod(RpcPacket packet) {
        super(packet);
    }

    @Override
    public void doCmd(ISession session) {
        log.debug("SyncCallBackMethod doCmd:cmd = {}",packet.getCmd());
        //根据消息ID查找Future
        SyncRpcCallBack callBack=session.getAttribute(Session.SYNC_CALLBACK_MAP).get(packet.getUuid());
        if(callBack!=null){
            if(packet.isException()){
                callBack.processException(new RemoteException((Throwable) packet.getParams()[0]));
                log.error("SyncCallBackMethod process exception:cmd = {}", packet.getCmd());
                return;
            }
            if(packet.getParams().length==1&&packet.getClassNameArray().length==1){
                callBack.processResult(session,packet.getParams()[0]);
            }else{
                callBack.processResult(session, packet.getParams());

            }
            log.debug("SyncCallBackMethod process result:cmd = {}",packet.getCmd());
            return;
        }
    }
}
