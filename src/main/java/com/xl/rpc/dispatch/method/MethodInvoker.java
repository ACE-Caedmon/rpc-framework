package com.xl.rpc.dispatch.method;

import java.lang.reflect.Method;

/**
 * Created by Caedmon on 2015/8/22.
 */
public class MethodInvoker {
    private BeanAccess beanAccess;
    private Method method;
    private Class clazz;
    public MethodInvoker(BeanAccess beanAccess,Method method,Class clazz){
        this.beanAccess=beanAccess;
        this.method=method;
        this.clazz=clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }
    public Object invoke(Object[] params) throws Exception{
        Object target=this.beanAccess.getBean(clazz);
        return method.invoke(target,params);
    }
}
