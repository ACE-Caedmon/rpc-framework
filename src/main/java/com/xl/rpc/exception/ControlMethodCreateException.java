package com.xl.rpc.exception;

/**
 * Created by Caedmon on 2015/7/12.
 */
public class ControlMethodCreateException extends BaseException{
    public ControlMethodCreateException(Exception e) {
        super(e);
    }

    public ControlMethodCreateException(String s) {
        super(s);
    }
}
