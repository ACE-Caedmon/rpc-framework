package com.xl.dispatch.message;

import com.xl.annotation.MsgType;
import com.xl.codec.DefaultPracticalBuffer;
import com.xl.codec.PracticalBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Caedmon on 2015/7/12.
 * 全局消息体代理工厂，只允许存在一个实例
 */
public class MessageProxyFactory {
    private Map<MsgType,Map<Class,MessageProxy>> proxCacheByMsgType=new ConcurrentHashMap<>();
    private ClassPool classPool=ClassPool.getDefault();
    private static final String PROXY_SUFFIX="Proxy";
    public static MessageProxyFactory ONLY_INSTANCE=new MessageProxyFactory();
    public static final Map<Class,Class> primitiveClassCache=new HashMap<>();
    private Object lock=new Object();
    static {
        primitiveClassCache.put(Boolean.TYPE, Boolean.class);
        primitiveClassCache.put(Byte.TYPE, Byte.class);
        primitiveClassCache.put(Character.TYPE, Character.class);
        primitiveClassCache.put(Double.TYPE, Double.class);
        primitiveClassCache.put(Float.TYPE, Float.class);
        primitiveClassCache.put(Integer.TYPE, Integer.class);
        primitiveClassCache.put(Long.TYPE, Long.class);
        primitiveClassCache.put(Short.TYPE, Short.class);
    }
    private MessageProxyFactory(){

    }
    public MessageProxy getMessageProxy(MsgType type,Class clazz) throws Exception{
        Map<Class,MessageProxy> proxyCache=proxCacheByMsgType.get(type);
        MessageProxy proxy=null;
        if(proxyCache==null){
            synchronized (lock){
                if(proxyCache==null){
                    proxyCache=new ConcurrentHashMap<>();
                    proxCacheByMsgType.put(type,proxyCache);
                }
            }

        }
        proxy=proxyCache.get(clazz);
        if(proxy==null){
            proxy=createMessageProxy(type,clazz);
            proxyCache.put(clazz,proxy);
            if(primitiveClassCache.containsKey(clazz)){
                proxyCache.put(primitiveClassCache.get(clazz),proxy);
            }
            proxCacheByMsgType.put(type,proxyCache);
        }
        return proxy;
    }
    private MessageProxy createMessageProxy(MsgType type,Class clazz) throws Exception{
        String className=clazz.getName();
        Class primitiveClass=primitiveClassCache.get(clazz);
        //是否为基本数据类型 int,long等
        if(primitiveClass!=null){
            className=primitiveClass.getName();
        }
        MessageProxy proxy=null;
        switch (type){
            case JSON:
                //proxy=JSON_MESSAGE_PROXY_INSTANCE;
                String proxyClassName=(primitiveClass!=null?primitiveClass.getSimpleName():clazz.getSimpleName())+MsgType.JSON+PROXY_SUFFIX;
                CtClass ctClass=classPool.getOrNull(proxyClassName);
                if(ctClass==null){
                    synchronized (lock){
                        if(ctClass==null){
                            ctClass=classPool.getAndRename(MessageProxy.class.getName(),proxyClassName);
                            ctClass.setSuperclass(classPool.getCtClass(MessageProxy.class.getName()));
                            //encode $1 data
                            CtMethod decodeMethod=ctClass.getDeclaredMethod("decode");
                            String decodeMethodBody="{"+className+" result = ("+className+")$1.readJSON("+className+".class);return ("+className+")result;}";
                            decodeMethod.setBody(decodeMethodBody);
                            //encode $1 bean
                            CtMethod encodeMethod=ctClass.getDeclaredMethod("encode");
                            String encodeMethodBody="{"+
                                    ByteBuf.class.getName()+" buffer = "+
                                    Unpooled.class.getName()+".buffer();"+
                                    PracticalBuffer.class.getName()+" data=new "+DefaultPracticalBuffer.class.getName()+"(buffer);"+
                                    "data.writeJSON($1);return data;}";
                            encodeMethod.setBody(encodeMethodBody.toString());
                            ctClass.writeFile("javassit/");
                        }
                    }
                    proxy=(MessageProxy)ctClass.toClass().newInstance();
                }
                break;
            case Binary:
                break;
            case ProtoBuf:
                break;
            default:
                break;
        }
        return proxy;
    }

}
