package com.xl.cluster.client;

import com.xl.annotation.CmdControl;
import com.xl.annotation.CmdMethod;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/17.
 */
public class JavassitRpcCallProxyFactory implements RpcCallProxyFactory{
    private Map<Class,Object> callProxyCache=new HashMap<>();
    private ClassPool classPool=ClassPool.getDefault();
    private static final String PROXY_SUFFIX="RpcCallProxy";
    public <T> T getRpcCallProxy(Class<T> callInterface){
        T proxy=(T)callProxyCache.get(callInterface);
        if(proxy==null){
            try{
                proxy=createCallProxy(callInterface);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
            callProxyCache.put(callInterface,proxy);
        }
        return proxy;
    }
    private <T> T createCallProxy(Class<T> clazz) throws Exception{
        String className=clazz.getName();
        //判断注解
        CmdControl rpcControlAnnotation=clazz.getAnnotation(CmdControl.class);
        if(rpcControlAnnotation==null){
            throw new IllegalArgumentException("未配置注解@CmdControl:class = "+className);
        }
        String clusterName=rpcControlAnnotation.value();
        if(clusterName==null||clusterName.trim().equals("")){
            throw new IllegalArgumentException("未指定集群组名: class = "+className);
        }
        T proxy=null;
        String proxyClassName=clazz.getSimpleName()+PROXY_SUFFIX;
        CtClass ctClass=classPool.getOrNull(proxyClassName);
        if(ctClass==null){
            ctClass= classPool.makeClass(proxyClassName);
            ctClass.setInterfaces(new CtClass[]{classPool.getCtClass(clazz.getName())});
            Method[] methods=clazz.getDeclaredMethods();
            int i=0;
            //查看带注解的方法
            for(Method method:methods){
                CmdMethod cmdMethod=method.getAnnotation(CmdMethod.class);
                int cmd=cmdMethod.cmd();
                Class returnType=method.getReturnType();
                StringBuilder body=new StringBuilder();
                //void 方法
                if(returnType==Void.class||returnType==Void.TYPE){
                    body.append(SimpleRpcClientApi.class.getName() + ".getInstance().asyncRpcCall(\"" + clusterName + "\"," + cmd+",");
                    Class[] paramTypes=method.getParameterTypes();
                    StringBuilder params=new StringBuilder();
                    if(paramTypes.length<=0){
                        params.append(",null");
                    }else{
                        params.append("new Object[]{");
                        for(int m=0;m<paramTypes.length;m++){
                            if(m<paramTypes.length-1){
                                params.append("$"+(m+1)+",");
                            }else{
                                params.append("$"+(m+1));
                                params.append("}");
                            }
                        }
                    }
                    params.append(")");
                    body.append(params.toString() + ";");

                }else{
                    body.append(SimpleRpcClientApi.class.getName() + ".getInstance().syncRpcCall(\"" + clusterName + "\"," + cmd+","+returnType.getName()+".class,");
                    Class[] paramTypes=method.getParameterTypes();
                    StringBuilder params=new StringBuilder();
                    if(paramTypes.length<=0){
                        params.append(",null");
                    }else{
                        params.append("new Object[]{");
                        for(int m=0;m<paramTypes.length;m++){
                            if(m<paramTypes.length-1){
                                params.append("$"+(m+1)+",");
                            }else{
                                params.append("$" + (m + 1));
                                params.append("}");
                            }
                        }
                    }
                    params.append(")");
                    body.append(params.toString() +";");
                    body.insert(1,returnType.getName()+" result =("+returnType.getName()+")");
                    body.append("return result;");
                }
                System.out.println(body.toString());
                CtMethod ctMethod=CtMethod.make(body.toString(),ctClass);
                i++;
                ctClass.addMethod(ctMethod);
            }
            proxy=(T)ctClass.toClass().newInstance();

        }
        return proxy;
    }

    @Override
    public <T> T createRpcCallProxy(Class<T> clazz) {
        return null;
    }
}
