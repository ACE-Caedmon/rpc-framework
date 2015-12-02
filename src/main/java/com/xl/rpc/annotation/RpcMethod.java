package com.xl.rpc.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/7/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RpcMethod {
    String value();
    float version() default 1.0f;
}