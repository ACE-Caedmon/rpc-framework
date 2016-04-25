package com.xiaoluo.rpc.exception;

/**
 * Created by Administrator on 2015/7/15.
 */
public class ClusterNodeException extends ClusterException{
    public ClusterNodeException(Exception e) {
        super(e);
    }

    public ClusterNodeException(String s) {
        super(s);
    }
}
