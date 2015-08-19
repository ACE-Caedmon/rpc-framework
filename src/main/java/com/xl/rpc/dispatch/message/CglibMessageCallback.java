package com.xl.rpc.dispatch.message;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.xl.message.LoginProtoBuffer;
import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.codec.DefaultPracticalBuffer;
import com.xl.rpc.codec.PracticalBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/8/19.
 */
public class CglibMessageCallback implements MethodInterceptor{
    private static final Logger log= LoggerFactory.getLogger(CglibMessageCallback.class);
    private Class clazz;
    private MsgType msgType;

    private static Map<String,ProtobufMapper> protobufMapperCache=new HashMap<>();
    private static class ProtobufMapper{
        String builderClassName;
        String protoClassName;
    }
    public CglibMessageCallback(MsgType msgType,Class clazz){
        this.clazz=clazz;
        this.msgType=msgType;
        if(MessageOrBuilder.class.isAssignableFrom(clazz)){
            this.msgType=MsgType.ProtoBuf;
            log.debug(clazz.getName()+" will codec as protobuffer");
        }

    }
    @Override
    public Object intercept(Object o, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        switch (msgType){
            case JSON:
                if(method.getName().equals("decode")){
                    PracticalBuffer data=(PracticalBuffer)params[0];
                    return data.readJSON(clazz);
                }
                if(method.getName().equals("encode")){
                    Object param=params[0];
                    ByteBuf buf= PooledByteBufAllocator.DEFAULT.buffer();
                    PracticalBuffer buffer=new DefaultPracticalBuffer(buf);
                    buffer.writeJSON(param);
                    return buffer;
                }
                break;
            case ProtoBuf:
                String builderClassName=null;
                boolean firstProto=true;
                String protoClassName=null;
                String className=clazz.getName();
                if(protobufMapperCache.containsKey(className)){
                    builderClassName= protobufMapperCache.get(className).builderClassName;
                    protoClassName=protobufMapperCache.get(className).protoClassName;
                    firstProto=false;
                }else{
                    builderClassName=className;
                    if(AbstractMessage.Builder.class.isAssignableFrom(clazz)) {
                        int lastIndex = builderClassName.lastIndexOf("Builder");
                        protoClassName = builderClassName.substring(0, lastIndex-1);
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
                if(method.getName().equals("decode")){
                    PracticalBuffer data=(PracticalBuffer)params[0];
                    Method newBuilderMethod=Class.forName(protoClassName).getMethod("newBuilder");
                    Message.Builder builder=(Message.Builder)newBuilderMethod.invoke(null, null);
                    return data.readProtoBuf(builder);
                }
                if(method.getName().equals("encode")){
                    Message.Builder builder=(Message.Builder)params[0];
                    ByteBuf buf= PooledByteBufAllocator.DEFAULT.buffer();
                    PracticalBuffer buffer=new DefaultPracticalBuffer(buf);
                    buffer.writeProtoBuf(builder);
                    return buffer;
                }
                break;
            default:
                break;
        }

        return null;
    }
}
