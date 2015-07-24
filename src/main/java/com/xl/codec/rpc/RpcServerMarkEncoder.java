package com.xl.codec.rpc;

import com.xl.codec.RpcPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created by Administrator on 2015/7/24.
 */
public class RpcServerMarkEncoder extends MessageToMessageEncoder<RpcPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcPacket msg, List<Object> out) throws Exception {
        msg.setFromCall(false);
        out.add(msg);
    }
}
