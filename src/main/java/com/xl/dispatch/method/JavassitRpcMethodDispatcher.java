package com.xl.dispatch.method;

import com.xl.annotation.*;
import com.xl.codec.RpcPacket;
import com.xl.dispatch.MethodInterceptor;
import com.xl.dispatch.message.MessageProxyFactory;
import com.xl.exception.ControlMethodCreateException;
import com.xl.session.ISession;
import com.xl.utils.ClassUtils;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Caedmon on 2015/7/11.
 */
public class JavassitRpcMethodDispatcher implements RpcMethodDispatcher {
    private ClassPool classPool=new ClassPool();
    private BeanAccess beanAccess;
    private Map<Integer,ControlMethodProxyCreator> proxyCreatorMap =new HashMap<>();
    private List<MethodInterceptor> methodInterceptors=new ArrayList<>();
    private static final Logger log= LoggerFactory.getLogger(JavassitRpcMethodDispatcher.class);
    private ExecutorService threadPool;
    public JavassitRpcMethodDispatcher(int threadSize){
        this(new PrototypeBeanAccess(), threadSize);
    }
    public JavassitRpcMethodDispatcher(BeanAccess beanAccess, int threadSize){
        this.beanAccess=beanAccess;
        this.threadPool=Executors.newFixedThreadPool(threadSize, new ThreadFactory() {
            private AtomicInteger size=new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread=new Thread(r);
                thread.setName("Method-Dispatcher-"+size.incrementAndGet());
                if(thread.isDaemon()){
                    thread.setDaemon(false);
                }
                //thread.setPriority(9);
                return thread;
            }
        });
        classPool.appendSystemPath();
    }
    @Override
    public ControlMethod newControlMethodProxy(RpcPacket rpcPacket) {
        ControlMethodProxyCreator creator= proxyCreatorMap.get(rpcPacket.getCmd());
        ControlMethod proxy=null;
        if(creator!=null){
            proxy=creator.create(rpcPacket);
        }else{
            if(rpcPacket.isFromCall()){
                throw new IllegalArgumentException("未找到指令处理器:cmd = "+rpcPacket.getCmd());
            }
            boolean sync=rpcPacket.getSync();
            if(sync){
                proxy=new SyncControlMethod(rpcPacket);
            }else{
                proxy=new AsyncCallBackMethod(rpcPacket);
            }
        }
        return proxy;
    }

    @Override
    public void loadClasses(Class... classes) throws Exception{
        for(Class controlClass:classes){
            loadControlClass(controlClass);
            log.info("load control: "+controlClass.getName());
        }
    }
    private void loadControlClass(Class controlClass) throws Exception {
        Class controlInterface=null;
        for(Class c:controlClass.getInterfaces()){
            if(ClassUtils.hasAnnotation(c,CmdControl.class)){
                controlInterface=c;
                break;
            }
        }
        List<Method> cmdMethods= ClassUtils.findMethodsByAnnotation(controlInterface, CmdMethod.class);
        String proxyClassName=controlClass.getSimpleName()+"Proxy";
        for(Method method:cmdMethods){
            CmdMethod ma=method.getAnnotation(CmdMethod.class);
            //$1 session
            int cmd=ma.cmd();
            if(proxyCreatorMap.containsKey(cmd)){
                log.warn("重复创建方法代理: controlClass = " + controlClass.getName() + ",cmd = " + cmd + "");
            }else{
                //要去重
                CtClass ctProxyClass=classPool.getOrNull(proxyClassName + cmd);
                if(ctProxyClass==null){
                    ctProxyClass=classPool.getAndRename(NoOpControlMethod.class.getName(),proxyClassName+cmd);
                    CtField beanFactoryField=CtField.make("private static final " + beanAccess.getClass().getName() + " beanAccess= new " + beanAccess.getClass().getName() + "();", ctProxyClass);
                    ctProxyClass.addField(beanFactoryField);
                    CtField controlField=CtField.make("private "+controlClass.getName()+" control=("+controlClass.getName()+")this.beanAccess.getBean("+controlClass.getName()+".class);",ctProxyClass);
                    ctProxyClass.addField(controlField);
                    CtMethod ctMethod=ctProxyClass.getDeclaredMethod("doCmd");
                    StringBuilder methodBody=new StringBuilder();
                    methodBody.append(getMethodInvokeSrc(method));
                    CmdResponse cmdResponse=method.getAnnotation(CmdResponse.class);
                    Class responseType=method.getReturnType();
                    //将response构造成数组作为参数，否则会编译出错

                    //带有CmdResponse 一定要响应
                    if(cmdResponse!=null){
                        if(!ClassUtils.isVoidReturn(responseType)){
                            //判断接受的消息是否为同步消息
                            if(responseType==Object[].class){
                                methodBody.append(" packet.setParams(response);$1.writeAndFlush(packet);");
                            }else{
                                methodBody.append(" packet.setParams(new Object[]{response});$1.writeAndFlush(packet);");
                            }
                        }else{
                            methodBody.append(" packet.setParams(null);$1.writeAndFlush(packet);");
                        }
                    }
                    System.out.println(methodBody.toString());
                    ctMethod.insertAfter(methodBody.toString());
                    //ctProxyClass.writeFile("javassit/");
                    Class resultClass=ctProxyClass.toClass();
                    ControlMethodProxyCreator creator=buildMethodProxyCreator(resultClass);
                    proxyCreatorMap.put(cmd, creator);

                }else{
                    //已经加载过
                    throw new ControlMethodCreateException("类名重复: controlClass = "+controlClass.getName()+",cmd = "+cmd+"");
                }

            }

        }
    }
    private ControlMethodProxyCreator buildMethodProxyCreator(Class<ControlMethod> proxy) throws ControlMethodCreateException{
        ControlMethodProxyCreator creator=null;
        String creatorClassName=ControlMethodProxyCreator.class.getName()+"$"+proxy.getSimpleName();
        try{
            CtClass creatorClass=classPool.getAndRename(ControlMethodProxyCreator.class.getName(), creatorClassName);
            creatorClass.setSuperclass(classPool.getCtClass(ControlMethodProxyCreator.class.getName()));
            CtMethod createMethod=creatorClass.getDeclaredMethod("create");
            createMethod.setBody("{" +
                    proxy.getName()+" proxy=new "+proxy.getName()+"($1);return proxy;"+
                    "}");
            creatorClass.writeFile("javassit/");
            creator=(ControlMethodProxyCreator)creatorClass.toClass().newInstance();

        }catch (Exception e){
            throw new  ControlMethodCreateException(e);
        }
        return creator;
    }
    private String getMethodInvokeSrc(Method method) throws Exception{
        //$1 session
        StringBuilder invokeSrc=new StringBuilder();
        Annotation[][] parametersAnnotations=method.getParameterAnnotations();
        Class[] parameterTypes=method.getParameterTypes();
        String[] invokeParams=new String[parameterTypes.length];
        int paramsCount=0;
        for(int i=0;i<parameterTypes.length;i++){
            //参数类型
            Class parameterType=parameterTypes[i];
            Annotation[] annotations=parametersAnnotations[i];
            CmdRequest cmdRequest=null;
            CmdUser cmdUser=null;
            for(Annotation annotation:annotations){
                Class annotationClass=annotation.annotationType();
                if(annotationClass==CmdRequest.class){
                    cmdRequest=(CmdRequest)annotation;
                    break;
                }
                if(annotationClass==CmdUser.class){
                    cmdUser=(CmdUser)annotation;
                    break;
                }
            }
            if(cmdRequest!=null){
                //注册MessageProxy
                MsgType requestType = cmdRequest.type();
                MessageProxyFactory.ONLY_INSTANCE.getMessageProxy(requestType, parameterType);
                String paramClassName=parameterType.getName();
                invokeParams[i] = "(" +paramClassName+ ")"+"(this.packet.getParams()["+paramsCount+"])";
                paramsCount++;
            }
            if(cmdUser!=null){
                if (parameterType.isAssignableFrom(ISession.class)) {
                    invokeParams[i] = "$1";
                } else {
                    //获取User对象
                    invokeParams[i] = "this.getCmdUser($1)";
                }
                continue;
            }
        }

        String methodName=method.getName();

        //是否自动将返回值发送给客户端
        Annotation cmdResponse=ClassUtils.getAnnotation(method, CmdResponse.class);
        boolean isCmdResponse=(cmdResponse!=null);
        //判断返回值类型
        Class returnType=method.getReturnType();
        if(isCmdResponse&&!ClassUtils.isVoidReturn(returnType)){
            invokeSrc.append(returnType.getName())
                    .append(" response= ");
        }
        invokeSrc.append("this.control." + methodName + "(");
        for(int i=0;i<invokeParams.length;i++){
            invokeSrc.append(invokeParams[i]);
            if(i<invokeParams.length-1){
                invokeSrc.append(",");
            }
            if(i==invokeParams.length-1){
                invokeSrc.append(");");
            }
        }
        return invokeSrc.toString();
    }
    @Override
    public BeanAccess getBeanAccess() {
        return beanAccess;
    }

    @Override
    public void setBeanAccess(BeanAccess beanAccess) {
        this.beanAccess = beanAccess;
    }

    @Override
    public void addMethodInterceptor(MethodInterceptor interceptor) {
        methodInterceptors.add(interceptor);
    }

    @Override
    public List<MethodInterceptor> getCmdInterceptors() {
        return methodInterceptors;
    }

    @Override
    public void dispatch(final ControlMethod methodProxy, final ISession session) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                RpcPacket packet = methodProxy.packet;
                int cmd = packet.getCmd();
                boolean allowed = true;
                final List<MethodInterceptor> interceptors = methodInterceptors;
                try {
                    for (MethodInterceptor interceptor : interceptors) {
                        allowed = interceptor.beforeDoCmd(session, packet);
                        if (!allowed) {
                            log.warn("拦截请求:cmd = {},interceptor = {}", cmd, interceptor.getClass().getName());
                            return;
                        }
                    }

                    if (allowed) {
                        methodProxy.doCmd(session);
                        for (MethodInterceptor interceptor : interceptors) {
                            interceptor.afterDoCmd(session, packet);
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    for (MethodInterceptor interceptor : interceptors) {
                        interceptor.exceptionCaught(session, packet, e);
                        break;
                    }
                    //把异常发回给调用方
                    packet.setException(true);
                    packet.setParams(e);
                    try{
                        session.writeAndFlush(packet);
                    }catch (Exception e1){
                        e1.printStackTrace();
                    }
                }
            }
        });
    }
}
