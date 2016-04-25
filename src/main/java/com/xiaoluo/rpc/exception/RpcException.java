package com.xiaoluo.rpc.exception;

/**
 * Created by Administrator on 2015/7/16.
 */
public class RpcException extends Throwable{
    private int cmd;
    public RpcException(int cmd){
        this.cmd=cmd;
    }
    public RpcException() {
        super();
    }

    public RpcException(Throwable e) {
        super(e);
    }

    public RpcException(String s) {
        super(s);
    }

    public RpcException(String s, Exception e) {
        super(s, e);
    }
}
