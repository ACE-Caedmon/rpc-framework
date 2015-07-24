package com.xl.exception;

/**
 * Created by Administrator on 2015/7/15.
 */
public class ClusterException extends BaseException{
    public ClusterException(Exception e) {
        super(e);
    }

    public ClusterException(String s) {
        super(s);
    }
}
