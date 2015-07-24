package com.xl.dispatch.method;

import com.xl.annotation.MsgType;
import com.xl.exception.CodecException;
import com.xl.session.ISession;
import io.netty.util.concurrent.DefaultProgressivePromise;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Caedmon on 2015/7/14.
 */
public class SyncRpcCallBack<T> implements RpcCallback<T>{
    private DefaultProgressivePromise<T> progressPromise;
    public SyncRpcCallBack(DefaultProgressivePromise<T> progressPromise) {
        this.progressPromise = progressPromise;
    }
    public static boolean isEmpty(Object[] content){
        if(content==null){
            return true;
        }
        for(Object o:content){
            if(o!=null){
                return false;
            }
        }
        return true;
    }

    @Override
    public void processResult(ISession session, T result) {
        progressPromise.setSuccess(result);
    }

    @Override
    public void processException(Throwable throwable) {
        progressPromise.setFailure(throwable);
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return progressPromise.get(timeout,unit);
    }
}
