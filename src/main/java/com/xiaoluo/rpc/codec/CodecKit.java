package com.xiaoluo.rpc.codec;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.xiaoluo.rpc.annotation.MsgType;
import com.xiaoluo.rpc.exception.CodecException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Caedmon on 2015/8/22.
 */
public class CodecKit {
    private static final Logger log= LoggerFactory.getLogger(CodecKit.class);
    private static Map<String,ProtobufMapper> protobufMapperCache=new HashMap<>();
    private static class ProtobufMapper{
        String builderClassName;
        String protoClassName;
    }
    public static <T> T decode(MsgType msgType,Class<T> clazz,PracticalBuffer data) throws Exception{
        if(clazz==Void.class||clazz==Void.TYPE){
            return null;
        }
        msgType=getSuitableMsgType(msgType,clazz);
        switch (msgType){
            case JSON:
                return data.readJSON(clazz);
            case ProtoBuf:
                Method newBuilderMethod=Class.forName(getProtoClassName(clazz)).getMethod("newBuilder");
                Message.Builder builder=(Message.Builder)newBuilderMethod.invoke(null,null);
                return (T)data.readProtoBuf(builder);
            case JavaSerialization:
                int exceptionLen=data.readInt();
                byte[] bytes=new byte[exceptionLen];
                data.getByteBuf().readBytes(bytes);
                return CodecKit.derialize(bytes,clazz);
            case CustomParam:
                CodecMapper param=(CodecMapper)clazz.newInstance();
                param.decode(data);
                return (T)param;
            default:
                throw new IllegalArgumentException("Unsupport msgType "+msgType);
        }
    }
    public static PracticalBuffer encode(MsgType msgType,Object bean) throws Exception{
        msgType=getSuitableMsgType(msgType,bean.getClass());
        ByteBuf buf= Unpooled.buffer();
        PracticalBuffer buffer=new DefaultPracticalBuffer(buf);
        switch (msgType){
            case JSON:
                buffer.writeJSON(bean);
                break;
            case ProtoBuf:
                Message.Builder builder=(Message.Builder)bean;
                buffer.writeProtoBuf(builder);
                break;
            case JavaSerialization:
                byte[] bytes= CodecKit.serialize(bean);
                buffer.writeInt(bytes.length);
                buffer.writeBytes(bytes);
                break;
            case CustomParam:
                CodecMapper param=(CodecMapper)bean;
                buffer.writeBytes(param.encode());
                break;
            default:
                throw new IllegalArgumentException("Unsupport msgType "+msgType);
        }
        return buffer;
    }
    public static MsgType getSuitableMsgType(MsgType msgType,Class clazz){
        if(CodecMapper.class.isAssignableFrom(clazz)){
            return MsgType.CustomParam;
        }
        if(Throwable.class.isAssignableFrom(clazz)){
            return MsgType.JavaSerialization;
        }
        if(MessageOrBuilder.class.isAssignableFrom(clazz)){
            return MsgType.ProtoBuf;
        }
        return msgType;
    }
    public static byte[] serialize(Object object){
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos=new ObjectOutputStream(bos);
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CodecException(e);
        }finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static <T> T derialize(byte[] bytes,Class<T> clazz){
        ByteArrayInputStream bis=new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois=new ObjectInputStream(bis);
            return (T)ois.readObject();
        } catch (Exception e) {
            throw new CodecException(e);
        }finally {
            try {
                bis.close();
            } catch (IOException e) {
                throw new CodecException(e);
            }
        }
    }
    public static String getProtoClassName(Class clazz){
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
        return protoClassName;
    }
}
