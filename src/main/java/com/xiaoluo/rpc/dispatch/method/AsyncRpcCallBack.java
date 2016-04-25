package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.dispatch.ISession;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Administrator on 2015/7/24.
 */
public abstract class AsyncRpcCallBack<T> implements RpcCallback<T>{
    @Override
    public abstract void processResult(ISession session, T result);

    @Override
    public abstract void processException(Throwable throwable);

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
