package com.xl.rpc.dispatch.tcp;

import com.xl.rpc.boot.TCPClientSettings;
import com.xl.rpc.codec.rpc.RpcClientMarkEncoder;
import com.xl.rpc.codec.rpc.RpcDecoder;
import com.xl.rpc.codec.rpc.RpcEncoder;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.session.Session;
import com.xl.utils.EngineParams;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by Caedmon on 2015/4/25.
 */
public class TCPClientInitializer extends ChannelInitializer<SocketChannel>{
    private RpcMethodDispatcher dispatcher;
    private TCPClientSettings settings;
    public TCPClientInitializer(RpcMethodDispatcher dispatcher,TCPClientSettings settings){
        this.dispatcher = dispatcher;
        this.settings=settings;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        if(EngineParams.isNettyLogging()){
            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        }
        ch.pipeline().addLast(new TCPHeadDecoder());
        ch.pipeline().addLast(new RpcDecoder());
        ch.pipeline().addLast(new TCPHeadEncoder());
        ch.pipeline().addLast(new RpcEncoder());
        ch.pipeline().addLast(new RpcClientMarkEncoder());
        ch.pipeline().addLast(new TCPInboundHandler(dispatcher));
        ch.attr(Session.SECRRET_KEY).set(settings.secretKey);


    }
}
