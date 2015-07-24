package com.xl.exception;

/**
 * Created by Administrator on 2015/7/15.
 */
public class EngineException extends BaseException{
    public EngineException(Exception e) {
        super(e);
    }

    public EngineException(String s) {
        super(s);
    }

    public EngineException(String s, Exception e) {
        super(s, e);
    }
}
