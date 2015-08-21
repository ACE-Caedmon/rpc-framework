package com.xl.rpc.dispatch.message;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.cluster.client.RpcCallProxyFactory;
import com.xl.rpc.codec.DefaultPracticalBuffer;
import com.xl.rpc.codec.PracticalBuffer;
import com.xl.utils.ClassUtils;
import com.xl.utils.EngineParams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import net.sf.cglib.proxy.Enhancer;
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
    private static final Logger log= LoggerFactory.getLogger(MessageProxyFactory.class);
    private Object lock=new Object();

    private Map<String,ProtobufMapper> protobufMapperCache=new HashMap<>();
    private static class ProtobufMapper{
        String builderClassName;
        String protoClassName;
    }
    private MessageProxyFactory(){

    }
    public MessageProxy getMessageProxy(MsgType type,Class clazz) throws Exception{
        if(ClassUtils.isPrimitive(clazz)){
            throw new IllegalArgumentException("Currently not supported primitive type "+clazz.getName());
        }
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
                    proxy=createCglibMessageProxy(type,clazz);
                    proxyCache.put(clazz,proxy);
                    Class keyClass=ClassUtils.getPackingType(clazz);
                    proxyCache.put(keyClass,proxy);
                    proxCacheByMsgType.put(type,proxyCache);
                }
            }
        }
        return proxy;
    }
    public MessageProxy createCglibMessageProxy(MsgType msgType,Class clazz) {
        Enhancer syncEnhancer = new Enhancer();//通过类Enhancer创建代理对象
        syncEnhancer.setSuperclass(MessageProxy.class);//传入创建代理对象的类
        syncEnhancer.setCallback(new CglibMessageCallback(msgType,clazz));//设置回调
        MessageProxy messageProxy=(MessageProxy)syncEnhancer.create();//创建代理对象
        log.info("Create RpcCallProxy instance :{}", messageProxy.getClass().getName());
        return messageProxy;
    }
    private  MessageProxy createMessageProxy(MsgType type,Class clazz) throws Exception{
        String className= ClassUtils.getCompleteClassName(clazz);
        MessageProxy proxy=null;
        String proxyClassName=className+type+PROXY_SUFFIX;
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
                decodeMethodBody="{Object result = $1.readJSON("+className+".class);return result;}";
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
