package com.xl.rpc.exception;

/**
 * Created by Administrator on 2015/7/29.
 */
public class RemoteException extends RpcException{
    public RemoteException(Throwable e) {
        super(e);
    }
}
