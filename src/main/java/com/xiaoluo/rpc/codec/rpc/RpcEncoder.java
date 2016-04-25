package com.xiaoluo.rpc.codec.rpc;


import com.xiaoluo.rpc.boot.EngineSettings;
import com.xiaoluo.rpc.codec.*;
import com.xiaoluo.rpc.registry.RegistryConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

            ByteBuf buf= Unpooled.buffer();
            DefaultPracticalBuffer data=new DefaultPracticalBuffer(buf);
            data.writeFloat(EngineSettings.VERSION);
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
                    PracticalBuffer elementBuf=CodecKit.encode(packet.getMsgType(), e);
                    data.writeBytes(elementBuf);
                }
            }
            BinaryPacket nextPacket=new BinaryPacket(buf);
            out.add(nextPacket);
            if(!packet.getCmd().equals(RegistryConstant.MonitorServerMethod.HEART_BEAT)) {
                log.debug("Rpc encode {}:{}", ctx.channel(), packet.toString());
            }
        }catch (Error e){
            e.printStackTrace();
            log.error("Rpc encode error {}",ctx.channel(),e);
        }
	}
}
