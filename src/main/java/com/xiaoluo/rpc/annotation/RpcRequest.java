package com.xiaoluo.rpc.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/7/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
@Deprecated
public @interface RpcRequest {
    MsgType value() default MsgType.JSON;
}
