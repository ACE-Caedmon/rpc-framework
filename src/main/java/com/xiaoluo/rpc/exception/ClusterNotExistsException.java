package com.xiaoluo.rpc.exception;

/**
 * Created by Administrator on 2015/8/17.
 */
public class ClusterNotExistsException extends ClusterException{
    public ClusterNotExistsException(Exception e) {
        super(e);
    }

    public ClusterNotExistsException(String s) {
        super(s);
    }

    public ClusterNotExistsException(String s, Exception e) {
        super(s, e);
    }
}
