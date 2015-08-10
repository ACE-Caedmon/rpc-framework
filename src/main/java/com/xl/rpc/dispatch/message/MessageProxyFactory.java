package com.xl.rpc.dispatch.message;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.codec.DefaultPracticalBuffer;
import com.xl.rpc.codec.PracticalBuffer;
import com.xl.utils.EngineParams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Caedmon on 2015/7/12.
 * 全局消息体代理工厂，只允许存在一个实例
 */
public class MessageProxyFactory {
    private Map<MsgType,Map<Class,MessageProxy>> proxCacheByMsgType=new ConcurrentHashMap<>();
    private ClassPool classPool=ClassPool.getDefault();
    private static final String PROXY_SUFFIX="Proxy";
    private static final String BUFFER_INIT_CODE=ByteBuf.class.getName()+" buffer = "+
            Unpooled.class.getName()+".buffer();"+
            PracticalBuffer.class.getName()+" data=new "+DefaultPracticalBuffer.class.getName()+"(buffer);";
    public static MessageProxyFactory ONLY_INSTANCE=new MessageProxyFactory();
    public static final Map<Class,Class> PRIMITIVE_CLASS_CACHE =new HashMap<>();
    public static final Set<String> INVAILD_PACKAGE_NAMES=new HashSet<>();
    private static final Logger log= LoggerFactory.getLogger(MessageProxyFactory.class);
    private Object lock=new Object();
    static {
        PRIMITIVE_CLASS_CACHE.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_CLASS_CACHE.put(Byte.TYPE, Byte.class);
        PRIMITIVE_CLASS_CACHE.put(Character.TYPE, Character.class);
        PRIMITIVE_CLASS_CACHE.put(Double.TYPE, Double.class);
        PRIMITIVE_CLASS_CACHE.put(Float.TYPE, Float.class);
        PRIMITIVE_CLASS_CACHE.put(Integer.TYPE, Integer.class);
        PRIMITIVE_CLASS_CACHE.put(Long.TYPE, Long.class);
        PRIMITIVE_CLASS_CACHE.put(Short.TYPE, Short.class);
        INVAILD_PACKAGE_NAMES.add("java.lang.");
        INVAILD_PACKAGE_NAMES.add("java.util.");
    }
    private Map<String,ProtobufMapper> protobufMapperCache=new HashMap<>();
    private static class ProtobufMapper{
        String builderClassName;
        String protoClassName;
    }
    private MessageProxyFactory(){

    }
    public MessageProxy getMessageProxy(MsgType type,Class clazz) throws Exception{
        Map<Class,MessageProxy> proxyCache=proxCacheByMsgType.get(type);
        MessageProxy proxy=null;
        if(proxyCache==null){
            synchronized (lock){
                proxyCache=proxCacheByMsgType.get(type);
                if(proxyCache==null){
                    proxyCache=new ConcurrentHashMap<>();
                    proxCacheByMsgType.put(type,proxyCache);
                }
            }
        }
        proxy=proxyCache.get(clazz);
        if(proxy==null){
            synchronized (lock){
                proxy=proxyCache.get(clazz);
                if(proxy==null){
                    proxy=createMessageProxy(type,clazz);
                    proxyCache.put(clazz,proxy);
                    if(PRIMITIVE_CLASS_CACHE.containsKey(clazz)){
                        proxyCache.put(PRIMITIVE_CLASS_CACHE.get(clazz),proxy);
                    }
                    proxCacheByMsgType.put(type,proxyCache);
                }
            }
        }
        return proxy;
    }
    private  MessageProxy createMessageProxy(MsgType type,Class clazz) throws Exception{
        String className=clazz.getName();
        Class primitiveClass= PRIMITIVE_CLASS_CACHE.get(clazz);
        MessageProxy proxy=null;
        String proxyClassNamePrefix=className;
        //是否为基本数据类型 int,long等
        if(primitiveClass!=null){
            proxyClassNamePrefix=primitiveClass.getName();
        }
        for(String packageName:INVAILD_PACKAGE_NAMES){
            if(className.startsWith(packageName)){
                proxyClassNamePrefix=className.replaceFirst(packageName,"");
                break;
            }
        }
        String proxyClassName=proxyClassNamePrefix+type+PROXY_SUFFIX;
        CtClass ctClass=classPool.getOrNull(proxyClassName);
        if(ctClass!=null){
            throw new IllegalStateException("MessageProxy has exists:"+proxyClassName);
        }
        ctClass=classPool.getAndRename(MessageProxy.class.getName(),proxyClassName);
        ctClass.setSuperclass(classPool.getCtClass(MessageProxy.class.getName()));
        //encode $1 data
        CtMethod decodeMethod=ctClass.getDeclaredMethod("decode");
        CtMethod encodeMethod=ctClass.getDeclaredMethod("encode");
        String decodeMethodBody=null;
        String encodeMethodBody=null;
        if(MessageOrBuilder.class.isAssignableFrom(clazz)){
            type=MsgType.ProtoBuf;
            log.debug(className+" auto decode as a protobuf");
        }
        switch (type){
            case JSON:
                decodeMethodBody="{"+className+" result = ("+className+")$1.readJSON("+className+".class);return ("+className+")result;}";
                encodeMethodBody="{"+BUFFER_INIT_CODE+
                        "data.writeJSON($1);return data;}";
                break;
            case Binary:
                break;
            case ProtoBuf:
                String builderClassName=null;
                boolean firstProto=true;
                String protoClassName=null;
                if(protobufMapperCache.containsKey(className)){
                    builderClassName= protobufMapperCache.get(className).builderClassName;
                    protoClassName=protobufMapperCache.get(className).protoClassName;
                    firstProto=false;
                }else{
                    builderClassName=className.replaceAll("\\$",".");
                    if(AbstractMessage.Builder.class.isAssignableFrom(clazz)) {
                        int lastIndex = builderClassName.lastIndexOf("Builder");
                        protoClassName = builderClassName.substring(0, lastIndex);
                        protoClassName = protoClassName.replaceAll("\\$", ".");
                        firstProto=true;
                    }else{
                        throw new UnsupportedOperationException("Unsupported this protobuf:" + className);
                    }
                }
                if(firstProto){
                    ProtobufMapper mapper=new ProtobufMapper();
                    mapper.builderClassName=builderClassName;
                    mapper.protoClassName=protoClassName;
                    protobufMapperCache.put(className, mapper);
                }
                decodeMethodBody="{return $1.readProtoBuf("+protoClassName+"newBuilder());}";
                encodeMethodBody="{"+BUFFER_INIT_CODE+" data.writeProtoBuf(("+builderClassName+")$1);return data;"+"}";
                break;
            default:
                break;
        }
        decodeMethod.setBody(decodeMethodBody);
        encodeMethod.setBody(encodeMethodBody);
        if(EngineParams.isWriteJavassit()){
            ctClass.writeFile("javassit/");
        }
        proxy=(MessageProxy)ctClass.toClass().newInstance();
        log.debug("Create MessageProxy instance :{}",proxy.getClass().getName());
        return proxy;
    }

}
