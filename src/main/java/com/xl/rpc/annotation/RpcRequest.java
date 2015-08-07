package com.xl.rpc.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/7/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface RpcRequest {
    MsgType value() default MsgType.JSON;
}
