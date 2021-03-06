package com.xiaoluo.rpc.codec.rpc;

import com.xiaoluo.rpc.codec.RpcPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created by Administrator on 2015/7/24.
 */
public class RpcClientMarkEncoder extends MessageToMessageEncoder<RpcPacket>{
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcPacket msg, List<Object> out) throws Exception {
        msg.setFromCall(true);
        out.add(msg);
    }
}
