package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.internal.SpringBeanAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.Method;

/**
 * Created by Caedmon on 2015/8/22.
 */
public class MethodInvoker {
    private BeanAccess beanAccess;
    private Method method;
    private Class clazz;
    private static Logger log= LoggerFactory.getLogger(MethodInvoker.class);
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
        Object target=null;
        try{
            target=this.beanAccess.getBean(clazz);
        }catch (NoSuchBeanDefinitionException e){
            if(beanAccess instanceof SpringBeanAccess){
               target=clazz.newInstance();
                log.warn("Spring context can not find this bean:{},default call class.newInstance() ",clazz.getName());
            }else{
                throw e;
            }
        }

        return method.invoke(target,params);
    }
}
