package com.xl.rpc.boot;

import com.xl.rpc.dispatch.tcp.TCPClientInitializer;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.session.ISession;
import com.xl.session.Session;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Caedmon on 2015/4/25.
 */
public class RpcClientSocketEngine extends SocketEngine{
    private static final Logger log= LoggerFactory.getLogger(RpcClientSocketEngine.class);
    private EventLoopGroup eventExecutors;
    private Channel channel;
    private Bootstrap bootstrap;
    public RpcClientSocketEngine(TCPClientSettings settings, RpcMethodDispatcher rpcMethodDispatcher) {
        super(settings, rpcMethodDispatcher);
    }
    public RpcClientSocketEngine(TCPClientSettings settings, RpcMethodDispatcher rpcMethodDispatcher, EventLoopGroup eventExecutors) {
        super(settings, rpcMethodDispatcher);
        this.settings=settings;
        this.eventExecutors=eventExecutors;
    }
    public void connect() throws Exception{
        this.connect(((TCPClientSettings) settings).host, settings.port);
    }
    public void connect(String host, int port) throws Exception{
        ChannelFuture f =this.bootstrap.connect(host,port);
        ChannelFuture future=f.sync();
        future.get();
        this.channel=f.channel();
    }
    @Override
    public void startSocket() throws Exception{
        String host=((TCPClientSettings) settings).host;
        int port=settings.port;
        log.info("=========Rpc client connect to {}:{}===========", host, port);
        EventLoopGroup workerGroup=null;
        if(this.eventExecutors==null){
            workerGroup= new NioEventLoopGroup(settings.workerThreadSize);
        }else{
            workerGroup=this.eventExecutors;
        }
        ChannelInitializer<SocketChannel> initializer=new TCPClientInitializer(this.rpcMethodDispatcher,(TCPClientSettings)settings);
        this.bootstrap= new Bootstrap();
        this.bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(initializer);
        try{
            connect();
        }catch (Exception e){
            log.info("Rpc client connect to {} error!",host+":"+settings.port);
            throw e;
        }
        log.debug("Worker thread : {}",settings.workerThreadSize);
        log.debug("Logic thread:{}",settings.cmdThreadSize);
        log.info("================Rpc client connect complete !===============");
    }
    public Channel getChannel(){
        return this.channel;
    }

    public ISession getSession(){
        return channel.attr(Session.SESSION_KEY).get();
    }
}
