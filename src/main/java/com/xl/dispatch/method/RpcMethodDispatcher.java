package com.xl.dispatch.method;

import com.xl.codec.RpcPacket;
import com.xl.dispatch.MethodInterceptor;
import com.xl.dispatch.method.BeanAccess;
import com.xl.dispatch.method.ControlMethod;
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
    void addMethodInterceptor(MethodInterceptor interceptor);
    List<MethodInterceptor> getCmdInterceptors();
    void dispatch(ControlMethod methodProxy,ISession session);
}
