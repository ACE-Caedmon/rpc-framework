package com.xl.dispatch.method;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Administrator on 2015/7/13.
 */
public class SpringBeanAccess implements BeanAccess{
    public static final ClassPathXmlApplicationContext context=new ClassPathXmlApplicationContext("spring/server-beans.xml");
    @Override
    public <T> T getBean(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return context.getBean(clazz);
    }
}
