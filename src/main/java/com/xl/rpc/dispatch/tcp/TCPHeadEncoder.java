package com.xl.rpc.dispatch.tcp;

import com.xl.rpc.codec.BinaryPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by Caedmon on 2015/4/14.
 * 将BinaryPacket 转化为ByteBuf
 */
public class TCPHeadEncoder extends MessageToByteEncoder<BinaryPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BinaryPacket packet, ByteBuf out) throws Exception {
        ByteBuf buf=Unpooled.buffer();
        buf.writeInt(packet.getContent().readableBytes());
        buf.writeBytes(packet.getContent());
        out.writeBytes(buf);
        packet.getContent().release();
    }
}
