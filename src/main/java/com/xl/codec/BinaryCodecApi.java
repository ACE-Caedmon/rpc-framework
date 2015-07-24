package com.xl.codec;
import com.xl.codec.binary.BinaryEncryptUtil;
import com.xl.codec.binary.BinaryPacket;
import com.xl.dispatch.message.MessageProxy;
import com.xl.dispatch.message.MessageProxyFactory;
import com.xl.session.ISession;
import com.xl.session.Session;
import com.xl.utils.CommonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Administrator on 2015/4/25.
 */
public class BinaryCodecApi {
    private static Logger log= LoggerFactory.getLogger(BinaryCodecApi.class);
    public static class EncryptBinaryPacket {
        public byte[] content;
        public int passwordIndex;
        public boolean isEncrypt;
    }
    public static EncryptBinaryPacket encodeBody(RpcPacket packet, Channel channel) throws Exception{
        boolean isEncrypt=false;
        ISession session=channel.attr(Session.SESSION_KEY).get();
        if(session.containsAttribute(Session.NEED_ENCRYPT)){
            isEncrypt=session.getAttribute(Session.NEED_ENCRYPT);
        }
        ByteBuf buf=PooledByteBufAllocator.DEFAULT.buffer();
        ByteDataBuffer data=new ByteDataBuffer(buf);
        data.writeBoolean(packet.isFromCall());
        data.writeBoolean(packet.getSync());
        data.writeInt(packet.getCmd());
        data.writeString(packet.getUuid());
        data.writeBoolean(packet.isException());
        data.writeInt(packet.getMsgType().value);
        Object[] params=packet.getParams();
        StringBuilder classNameArray=new StringBuilder();
        String classNameResult;
        if(params!=null){
            for(Object e:params){
                if(e!=null){
                    classNameArray.append(",").append(e.getClass().getName());;
                }else{
                    classNameArray.append(",null");
                }
            }
            classNameResult=classNameArray.substring(1);
        }else{
            classNameResult="null";
        }
        data.writeString(classNameResult);
        if(params!=null){
            for(Object e:params){
                if(e!=null){
                    //如果是异常，则采用Java序列化方式
                    if(packet.isException()&&e instanceof Throwable){
                        byte[] bytes=CommonUtils.serialize(e);
                        data.writeInt(bytes.length);
                        data.writeBytes(bytes);
                    }else{
                        MessageProxy proxy= MessageProxyFactory.ONLY_INSTANCE.getMessageProxy(packet.getMsgType(), e.getClass());
                        data.writeBytes(proxy.encode(e));
                    }

                }
            }
        }
        byte[] dst=new byte[data.getByteBuf().readableBytes()];
        data.readBytes(dst);
        int passwordIndex=new Random().nextInt(256);
        if(isEncrypt){
            List<Short> passports=session.getAttribute(Session.PASSPORT);
            short passport=passports.get(passwordIndex);//根据索引获取密码
            String secretKey=channel.attr(Session.SECRRET_KEY).get();
            BinaryEncryptUtil.encode(dst, dst.length, secretKey, passport);//加密
        }
        EncryptBinaryPacket encryptPacket=new EncryptBinaryPacket();
        encryptPacket.content=dst;
        encryptPacket.passwordIndex=passwordIndex;
        encryptPacket.isEncrypt=isEncrypt;
        buf.release();
        return encryptPacket;

    }
    public static ByteDataBuffer decodeBody(BinaryPacket packet, ChannelHandlerContext ctx){
        ByteBuf content=packet.getContent();
        int length=content.readableBytes();
        int hasReadLength=0;
        //是否加密
        boolean isEncrypt=content.readBoolean();
        hasReadLength+=1;
        ISession session=ctx.channel().attr(Session.SESSION_KEY).get();
        byte entryptOffset=content.readByte();
        hasReadLength+=1;
        ByteBuf result= Unpooled.buffer();//.DEFAULT.buffer();//用来缓存一条报文的ByteBuf
        if(isEncrypt){
            byte[] dst=new byte[length-hasReadLength];//存储包体
            content.readBytes(dst);//读取包体内容
            short index = (short) (entryptOffset < 0 ? (256 + entryptOffset): entryptOffset);//获取密码表索引
            List<Short> passportList=session.getAttribute(Session.PASSPORT);//得到密码表集合
            short passport=passportList.get(index);//得到密码
            String secretKey=ctx.channel().attr(Session.SECRRET_KEY).get();
            BinaryEncryptUtil.decode(dst, dst.length, secretKey, passport);//解密
            result.writeBytes(dst);
        }else {
            result.writeBytes(content, length - hasReadLength);
        }
        return new ByteDataBuffer(result);
    }
}
