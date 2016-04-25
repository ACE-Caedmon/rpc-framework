package com.xiaoluo.rpc.exception;

/**
 * Created by Administrator on 2015/7/14.
 */
public class BaseException extends RuntimeException{
    public BaseException(Exception e){
        super(e);
    }
    public BaseException(String s){
        super(s);
    }
    public BaseException(String s,Exception e){
        super(s,e);
    }
}
