package com.xl.rpc.codec.rpc;


import com.xl.rpc.codec.BinaryPacket;
import com.xl.rpc.codec.DefaultPracticalBuffer;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.codec.CodecKit;
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
            data.writeInt(params.length);
            for(int i=0;i<params.length;i++){
                Object e=params[i];
                boolean isNull=e==null;
                data.writeBoolean(isNull);
                if(!isNull){
                    data.writeString(e.getClass().getName());
                    data.writeBytes(CodecKit.encode(packet.getMsgType(),e));
                }
            }
            BinaryPacket nextPacket=new BinaryPacket(buf);
            out.add(nextPacket);
            log.debug("Rpc encode :{}",packet.toString());
        }catch (Exception e){
            e.printStackTrace();
            log.error("Rpc encode error ",e);
        }
	}
}
