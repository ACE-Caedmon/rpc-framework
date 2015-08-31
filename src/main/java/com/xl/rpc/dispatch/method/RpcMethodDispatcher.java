package com.xl.rpc.dispatch.method;

import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.RpcMethodInterceptor;
import com.xl.rpc.exception.MethodInterceptException;
import com.xl.session.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Caedmon on 2015/7/11.
 */
public abstract class RpcMethodDispatcher {
    protected BeanAccess beanAccess;
    protected List<RpcMethodInterceptor> interceptors=new ArrayList<>();
    private static final Logger log= LoggerFactory.getLogger(RpcMethodDispatcher.class);
    protected ExecutorService threadPool;
    private static SyncMethodExecutor syncMethodExecutor=new SyncMethodExecutor();
    private static AsyncCallBackMethodExecutor asyncMethodExecutor=new AsyncCallBackMethodExecutor();
    public RpcMethodDispatcher(BeanAccess beanAccess, int threadSize){
        this.beanAccess=beanAccess;
        this.threadPool= Executors.newFixedThreadPool(threadSize, new ThreadFactory() {
            private AtomicInteger size = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Method-Dispatcher-" + size.incrementAndGet());
                if (thread.isDaemon()) {
                    thread.setDaemon(false);
                }
                //thread.setPriority(9);
                return thread;
            }
        });

    }
    public  abstract void loadClasses(Class... classes) throws Exception;
    public BeanAccess getBeanAccess(){
        return beanAccess;
    }
    public void setBeanAccess(BeanAccess beanAccess){
        this.beanAccess=beanAccess;
    }
    public void addMethodInterceptor(RpcMethodInterceptor interceptor){
        this.interceptors.add(interceptor);
    }
    public List<RpcMethodInterceptor> getCmdInterceptors(){
        return interceptors;
    }
    /**
     * 处理服务端回给客户端的响应
     * */
    public void processServerResponse(ISession session, RpcPacket packet){
        boolean sync=packet.getSync();
        if(sync){
            syncMethodExecutor.execute(session, packet);
        }else{
            asyncMethodExecutor.execute(session, packet);
        }
        log.debug("Process server response:cmd={}", packet.getCmd());
    }
    public void dispatch(final ISession session,final RpcPacket packet) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                if(packet.isFromCall()){
                    processClientRequest(session, packet);
                }else{
                    processServerResponse(session, packet);
                }
                after(session,packet);
            }
        });

    }
    protected Object before(ISession session,RpcPacket packet){
        for (RpcMethodInterceptor interceptor : interceptors) {
            if (!interceptor.beforeExecuteCmd(session, packet)) {
                log.warn("Cmd interceptor false:cmd = {},interceptor = {}", packet.getCmd(), interceptor.getClass().getName());
                return interceptor;
            }
        }
        return null;
    }
    protected void after(ISession session,RpcPacket packet) {
        for (RpcMethodInterceptor interceptor : interceptors) {
            interceptor.afterExecuteCmd(session, packet);
        }
    }
    protected void notifyError(ISession session,RpcPacket packet,Throwable throwable) {
        packet.setException(true);
        for (RpcMethodInterceptor interceptor : interceptors) {
            interceptor.exceptionCaught(session, packet, throwable);
            break;
        }
    }
    protected void responseToClient(ISession session, RpcPacket packet){
        Object[] params=packet.getParams();
        Object response;
        if(params==null){
            throw new NullPointerException("Params of packet can not be null");
        }
        response=params[0];
        if(response!=null){
            if(Throwable.class.isAssignableFrom(response.getClass())){
                packet.setException(true);
            }
            packet.setClassNameArray(new String[]{response.getClass().getName()});
        }
        session.writeAndFlush(packet);
    }
    public abstract void processClientRequest(ISession session, RpcPacket packet);
}
