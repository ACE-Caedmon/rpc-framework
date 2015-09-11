package com.xl.rpc.exception;

/**
 * Created by Administrator on 2015/9/11.
 */
public class VersionMisMatchException extends RuntimeException{
    public VersionMisMatchException(String s) {
        super(s);
    }
}
