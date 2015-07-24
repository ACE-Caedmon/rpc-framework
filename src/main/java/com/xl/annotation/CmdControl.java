package com.xl.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/4/3.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface CmdControl {
    //集群名
    String value() default "";
}
