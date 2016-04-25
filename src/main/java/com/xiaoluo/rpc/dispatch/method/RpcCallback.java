package com.xiaoluo.rpc.dispatch.method;

import com.xiaoluo.rpc.dispatch.ISession;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Administrator on 2015/7/23.
 */
public interface RpcCallback<T> {
    void processResult(ISession session, T result);
    void processException(Throwable throwable);
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
