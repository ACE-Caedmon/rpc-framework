package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.session.ISession;
import com.xl.session.Session;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/22.
 */
public class AsyncCallBackMethod extends ControlMethod<ISession> {
    private static final org.slf4j.Logger log= LoggerFactory.getLogger(AsyncCallBackMethod.class);
    public AsyncCallBackMethod(RpcPacket packet) {
        super(packet);
    }

    @Override
    public void doCmd(ISession session) {
        log.debug("AsyncCallBackMethod doCmd:cmd = {}",packet.getCmd());
        //根据消息ID查找Callback
        Map<String,RpcCallback> callbackMap=session.getAttribute(Session.ASYNC_CALLBACK_MAP);
        RpcCallback callBack=callbackMap.get(packet.getUuid());
        if(callBack!=null){
            try{
                Object[] params=packet.getParams();
                if(packet.isException()){
                    callBack.processException((Throwable) packet.getParams()[0]);
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

    @Override
    public ISession getCmdUser(ISession session) {
        return session;
    }
}
