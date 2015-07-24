package com.xl.cluster.event;

import com.xl.cluster.client.SimpleRpcClientApi;
import com.xl.event.IEventHandler;
import com.xl.session.ISession;

/**
 * Created by Administrator on 2015/7/21.
 */
public class RpcCloseEventHandler implements IEventHandler<ISession>{
    @Override
    public void handleEvent(ISession session) {
    }
}
