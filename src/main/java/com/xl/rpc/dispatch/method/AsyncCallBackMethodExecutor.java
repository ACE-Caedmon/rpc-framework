package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.exception.RemoteException;
import com.xl.session.ISession;
import com.xl.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/22.
 */
public class AsyncCallBackMethodExecutor extends NoOpMethodExecutor {
    private static final Logger log= LoggerFactory.getLogger(AsyncCallBackMethodExecutor.class);

    @Override
    public void execute(ISession session, RpcPacket packet) {
        log.debug("AsyncCallBackMethodExecutor execute:cmd = {}",packet.getCmd());
        //根据消息ID查找Callback
        Map<String,RpcCallback> callbackMap=session.getAttribute(Session.ASYNC_CALLBACK_MAP);
        RpcCallback callBack=callbackMap.get(packet.getUuid());
        if(callBack!=null){
            try{
                Object[] params=packet.getParams();
                if(packet.isException()){
                    callBack.processException(new RemoteException((Throwable) packet.getParams()[0]));
                    return;
                }
                if(params.length==1){
                    callBack.processResult(session,params[0]);
                }else{
                    callBack.processResult(session,params);
                }
                return;
            }finally {
                callbackMap.remove(packet.getUuid());
            }
        }
    }
}
