package com.xl.codec.rpc;


import com.xl.codec.DefaultPracticalBuffer;
import com.xl.codec.RpcPacket;
import com.xl.codec.BinaryPacket;
import com.xl.dispatch.message.MessageProxy;
import com.xl.dispatch.message.MessageProxyFactory;
import com.xl.utils.CommonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Sharable
public class RpcEncoder extends MessageToMessageEncoder<RpcPacket> {
    private static Logger log=LoggerFactory.getLogger(RpcEncoder.class);
    @Override
	protected void encode(ChannelHandlerContext ctx, RpcPacket packet, List<Object> out)
			throws Exception {
        try{
            ByteBuf buf=PooledByteBufAllocator.DEFAULT.buffer();
            DefaultPracticalBuffer data=new DefaultPracticalBuffer(buf);
            data.writeString(packet.getCmd());
            data.writeBoolean(packet.isFromCall());
            data.writeBoolean(packet.getSync());
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
            packet.setClassNameArray(classNameResult.split(","));
            data.writeString(classNameResult);
            if(params!=null){
                for(Object e:params){
                    if(e!=null){
                        //如果是异常，则采用Java序列化方式
                        if(packet.isException()&&e instanceof Throwable){
                            byte[] bytes= CommonUtils.serialize(e);
                            data.writeInt(bytes.length);
                            data.writeBytes(bytes);
                        }else{
                            MessageProxy proxy= MessageProxyFactory.ONLY_INSTANCE.getMessageProxy(packet.getMsgType(), e.getClass());
                            data.writeBytes(proxy.encode(e));

                        }

                    }
                }
            }
            BinaryPacket nextPacket=new BinaryPacket(buf);
            out.add(nextPacket);
            log.debug("Rpc encode :packet = {},time = {}",packet.toString(),System.currentTimeMillis());
        }catch (Exception e){
            e.printStackTrace();
            log.error("Rpc encode error ",e);
        }
	}
}
