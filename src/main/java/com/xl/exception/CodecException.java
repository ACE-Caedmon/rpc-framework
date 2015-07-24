package com.xl.exception;

/**
 * Created by Administrator on 2015/7/14.
 */
public class CodecException extends BaseException{
    public CodecException(Exception e) {
        super(e);
    }

    public CodecException(String s) {
        super(s);
    }
}
