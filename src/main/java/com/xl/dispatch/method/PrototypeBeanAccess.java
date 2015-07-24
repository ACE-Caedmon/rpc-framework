package com.xl.dispatch.method;

/**
 * Created by Caedmon on 2015/7/12.
 */
public class PrototypeBeanAccess implements BeanAccess {
    @Override
    public <T> T getBean(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }
}
