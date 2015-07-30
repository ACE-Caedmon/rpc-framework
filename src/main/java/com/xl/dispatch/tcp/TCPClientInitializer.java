package com.xl.dispatch.tcp;

import com.xl.boot.TCPClientSettings;
import com.xl.codec.rpc.RpcClientMarkEncoder;
import com.xl.codec.rpc.RpcDecoder;
import com.xl.codec.rpc.RpcEncoder;
import com.xl.dispatch.method.RpcMethodDispatcher;
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
    private RpcMethodDispatcher rpcMethodDispatcher;
    private TCPClientSettings settings;
    public TCPClientInitializer(RpcMethodDispatcher rpcMethodDispatcher,TCPClientSettings settings){
        this.rpcMethodDispatcher = rpcMethodDispatcher;
        this.settings=settings;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        if(EngineParams.isNettyLogging()){
            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        }
        ch.pipeline().addLast(new TCPHeadDecoder());
        ch.pipeline().addLast(new RpcDecoder(rpcMethodDispatcher));
        ch.pipeline().addLast(new TCPHeadEncoder());
        ch.pipeline().addLast(new RpcEncoder());
        ch.pipeline().addLast(new RpcClientMarkEncoder());
        ch.pipeline().addLast(new TCPInboundHandler(rpcMethodDispatcher));
        ch.attr(Session.SECRRET_KEY).set(settings.secretKey);


    }
}
