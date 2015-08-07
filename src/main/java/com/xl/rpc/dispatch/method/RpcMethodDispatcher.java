package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.CmdInterceptor;
import com.xl.session.ISession;

import java.util.List;

/**
 * Created by Caedmon on 2015/7/11.
 */
public interface RpcMethodDispatcher {
    ControlMethod newControlMethodProxy(RpcPacket packet);
    void loadClasses(Class... classes) throws Exception;
    BeanAccess getBeanAccess();
    void setBeanAccess(BeanAccess beanAccess);
    void addMethodInterceptor(CmdInterceptor interceptor);
    List<CmdInterceptor> getCmdInterceptors();
    void dispatch(ControlMethod methodProxy,ISession session);
}
