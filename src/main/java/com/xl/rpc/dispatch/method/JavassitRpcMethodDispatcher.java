package com.xl.rpc.dispatch.method;

import com.google.protobuf.MessageOrBuilder;
import com.xl.rpc.annotation.*;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.CmdInterceptor;
import com.xl.rpc.dispatch.message.MessageProxyFactory;
import com.xl.rpc.exception.ControlMethodCreateException;
import com.xl.rpc.internal.PrototypeBeanAccess;
import com.xl.session.ISession;
import com.xl.utils.ClassUtils;
import com.xl.utils.EngineParams;
import io.netty.util.internal.SystemPropertyUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Caedmon on 2015/7/11.
 */
public class JavassitRpcMethodDispatcher implements RpcMethodDispatcher {
    private ClassPool classPool=ClassPool.getDefault();
    private BeanAccess beanAccess;
    private Map<String,ControlMethodProxyCreator> proxyCreatorMap =new HashMap<>();
    private List<CmdInterceptor> cmdInterceptors =new ArrayList<>();
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
        String cmd=rpcPacket.getCmd();
        String classNames=rpcPacket.getClassNames();
        String methodKey=cmd+"-"+classNames;
        ControlMethodProxyCreator creator= proxyCreatorMap.get(methodKey);
        ControlMethod proxy=null;
        if(creator!=null){
            proxy=creator.create(rpcPacket);
        }else{
            if(rpcPacket.isFromCall()){
                throw new IllegalArgumentException("No method exists:method = "+methodKey);
            }
            boolean sync=rpcPacket.getSync();
            if(sync){
                proxy=new SyncControlMethod(rpcPacket);
            }else{
                proxy=new AsyncCallBackMethod(rpcPacket);
            }
        }
        log.info("Create method proxy:{}", proxy.getClass().getName());
        return proxy;
    }

    @Override
    public void loadClasses(Class... classes) throws Exception{
        for(Class controlClass:classes){
            loadControlClass(controlClass);
        }
    }
    private MsgType getMethodMsgType(Method method){
        Annotation[][] parametersAnnotations=method.getParameterAnnotations();
        Class[] parameterTypes=method.getParameterTypes();
        MsgType result=null;
        for(int i=0;i<parameterTypes.length;i++){
            //参数类型
            Class parameterType=parameterTypes[i];
            Annotation[] annotations=parametersAnnotations[i];
            RpcRequest rpcRequest =null;
            RpcSession rpcSession =null;
            for(Annotation annotation:annotations){
                Class annotationClass=annotation.annotationType();
                if(annotationClass==RpcRequest.class){
                    rpcRequest =(RpcRequest)annotation;
                    break;
                }
            }
            if(rpcRequest!=null){
                //检测到Protobuf结构作为参数，则将该方法协议结构类型定义为Protobuf
                if(MessageOrBuilder.class.isAssignableFrom(parameterType)){
                    MsgType paramterMsgType=MsgType.ProtoBuf;
                    if(result==null){
                        result=paramterMsgType;
                    }else{
                        if(result!=paramterMsgType){
                            throw new IllegalStateException("Except msgType is "+result+",actual msgType is "+paramterMsgType);
                        }
                    }
                }
            }
        }
        if(result==null){
            result=MsgType.JSON;
        }
        return result;
    }
    private void loadControlClass(Class controlClass) throws Exception {
        Class controlInterface=null;
        for(Class c:controlClass.getInterfaces()){
            if(ClassUtils.hasAnnotation(c,RpcControl.class)){
                controlInterface=c;
                break;
            }
        }
        List<Method> cmdMethods= ClassUtils.findMethodsByAnnotation(controlInterface, RpcMethod.class);
        String proxyClassName=controlClass.getSimpleName()+"Proxy";
        for(Method method:cmdMethods){
            RpcMethod ma=method.getAnnotation(RpcMethod.class);
            //$1 session
            String cmd=ma.value();
            if(proxyCreatorMap.containsKey(cmd)){
                log.warn("Repeated load control: controlClass = " + controlClass.getName() + ",cmd = " + cmd + "");
            }else{
                //要去重
                MsgType methodMsgType=getMethodMsgType(method);
                CtClass ctProxyClass=classPool.getOrNull(proxyClassName + cmd +methodMsgType);

                if(ctProxyClass==null){
                    ctProxyClass=classPool.getAndRename(NoOpControlMethod.class.getName(),proxyClassName+cmd+methodMsgType);
                    CtField beanFactoryField=CtField.make("private static final " + beanAccess.getClass().getName() + " beanAccess= new " + beanAccess.getClass().getName() + "();", ctProxyClass);
                    ctProxyClass.addField(beanFactoryField);
                    CtField controlField=CtField.make("private "+controlClass.getName()+" control=("+controlClass.getName()+")this.beanAccess.getBean("+controlClass.getName()+".class);",ctProxyClass);
                    ctProxyClass.addField(controlField);
                    CtMethod ctMethod=ctProxyClass.getDeclaredMethod("doCmd");
                    StringBuilder methodBody=new StringBuilder();
                    methodBody.append(getMethodInvokeSrc(method));
                    Class responseType=method.getReturnType();
                    if(!ClassUtils.isVoidReturn(responseType)){
                        //判断接受的消息是否为同步消息
                        methodBody.append("packet.setClassNameArray(new String[]{"+responseType.getName()+".class.getName()});");
                        if(responseType==Object[].class){
                            methodBody.append(" packet.setParams(response);$1.writeAndFlush(packet);");
                        }else{
                            methodBody.append(" packet.setParams(new Object[]{response});$1.writeAndFlush(packet);");
                        }
                    }else{
                        methodBody.append("packet.setClassNameArray(null);packet.setParams(null);$1.writeAndFlush(packet);");
                    }
                    log.debug("Javassit generate code:{}", methodBody.toString());
                    ctMethod.insertAfter(methodBody.toString());
                    if(EngineParams.isWriteJavassit()){
                        ctProxyClass.writeFile("javassit/");
                    }
                    Class resultClass=ctProxyClass.toClass();
                    ControlMethodProxyCreator creator=buildMethodProxyCreator(resultClass);

                    Class[] paramTypes=method.getParameterTypes();
                    List<String> classNames=new ArrayList<>(paramTypes.length);
                    int i=0;
                    for(Class paramType:paramTypes){
                        if(ISession.class.isAssignableFrom(paramType)){
                            continue;
                        }
                        classNames.add(ClassUtils.getPackingType(paramType).getName());
                    }
                    String creatorKey=cmd+"-"+ Arrays.toString(classNames.toArray());
                    if(proxyCreatorMap.containsKey(creatorKey)){
                        throw new IllegalStateException("Method creator has exists:controlClass = "+controlClass.getName()+",method = "+method.getName());
                    }
                    proxyCreatorMap.put(creatorKey, creator);
                    log.info("Cached method creator:key = {},method = {}.{}()",creatorKey,controlClass.getName(),method.getName());

                }else{
                    //已经加载过
                    log.debug("Class name has exists: controlClass = " + controlClass.getName() + ",cmd = " + cmd + "");
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
                    proxy.getName() + " proxy=new " + proxy.getName() + "($1);return proxy;" +
                    "}");
            if(EngineParams.isWriteJavassit()){
                creatorClass.writeFile("javassit/");
            }
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
            RpcRequest rpcRequest =null;
            RpcSession rpcSession =null;
            for(Annotation annotation:annotations){
                Class annotationClass=annotation.annotationType();
                if(annotationClass==RpcRequest.class){
                    rpcRequest =(RpcRequest)annotation;
                    break;
                }
                if(annotationClass==RpcSession.class){
                    rpcSession =(RpcSession)annotation;
                    break;
                }
            }
            if(rpcRequest !=null){
                //注册MessageProxy
                MsgType requestType = rpcRequest.value();
                MessageProxyFactory.ONLY_INSTANCE.getMessageProxy(requestType, parameterType);
                String paramClassName=parameterType.getName();
                if(ClassUtils.isPrimitive(parameterType)){
                    invokeParams[i]=ClassUtils.getPackingType(parameterType).getName()+".valueOf(this.packet.getParams()["+paramsCount+"].toString())."+paramClassName+"Value()";
                }else{
                    invokeParams[i] = "(" +paramClassName+ ")"+"(this.packet.getParams()["+paramsCount+"])";
                }
                paramsCount++;
            }
            if(rpcSession !=null){
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
        //判断返回值类型
        Class returnType=method.getReturnType();
        if(!ClassUtils.isVoidReturn(returnType)){
            String responseType=ClassUtils.getCompleteClassName(returnType);
            invokeSrc.append(responseType)
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
        System.out.println(invokeSrc.toString());
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
    public void addMethodInterceptor(CmdInterceptor interceptor) {
        cmdInterceptors.add(interceptor);
    }

    @Override
    public List<CmdInterceptor> getCmdInterceptors() {
        return cmdInterceptors;
    }

    @Override
    public void dispatch(final ControlMethod methodProxy, final ISession session) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                RpcPacket packet = methodProxy.packet;
                String cmd = packet.getCmd();
                boolean allowed = true;
                final List<CmdInterceptor> interceptors = cmdInterceptors;
                try {
                    for (CmdInterceptor interceptor : interceptors) {
                        allowed = interceptor.beforeDoCmd(session, packet);
                        if (!allowed) {
                            log.warn("Cmd interceptor false:cmd = {},interceptor = {}", cmd, interceptor.getClass().getName());
                            return;
                        }
                    }

                    if (allowed) {
                        methodProxy.doCmd(session);
                        for (CmdInterceptor interceptor : interceptors) {
                            interceptor.afterDoCmd(session, packet);
                            break;
                        }
                    }
                } catch (Throwable e) {
                    log.error("ControlMethod doCmd error:cmd = {}",cmd,e);
                    for (CmdInterceptor interceptor : interceptors) {
                        interceptor.exceptionCaught(session, packet, e);
                        break;
                    }
                    //把异常发回给调用方
                    packet.setException(true);
                    packet.setClassNameArray(new String[]{e.getClass().getName()});
                    packet.setParams(e);
                    try{
                        session.writeAndFlush(packet);
                    }catch (Exception e1){
                        e1.printStackTrace();
                        log.error("ControlMethod write exception error:cmd = {}",cmd,e);
                    }
                }
            }
        });
    }
}
