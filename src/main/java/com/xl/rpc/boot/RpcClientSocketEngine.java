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
 * Created by Administrator on 2015/4/25.
 */
public class RpcClientSocketEngine extends SocketEngine{
    private static final Logger log= LoggerFactory.getLogger(RpcClientSocketEngine.class);
    private EventLoopGroup eventExecutors;
    private Channel channel;
    public RpcClientSocketEngine(TCPClientSettings settings, RpcMethodDispatcher rpcMethodDispatcher) {
        super(settings, rpcMethodDispatcher);
    }
    public RpcClientSocketEngine(TCPClientSettings settings, RpcMethodDispatcher rpcMethodDispatcher, EventLoopGroup eventExecutors) {
        super(settings, rpcMethodDispatcher);
        this.settings=settings;
        this.eventExecutors=eventExecutors;
    }
    @Override
    public void startSocket() {
        EventLoopGroup workerGroup=null;
        if(this.eventExecutors==null){
            workerGroup= new NioEventLoopGroup(settings.workerThreadSize);
        }else{
            workerGroup=this.eventExecutors;
        }
        try {
            ChannelInitializer<SocketChannel> initializer=new TCPClientInitializer(this.rpcMethodDispatcher,(TCPClientSettings)settings);
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(initializer);
            ChannelFuture f =b.connect(((TCPClientSettings)settings).host,settings.port);
            ChannelFuture future=f.sync();
            future.get();
            this.channel=f.channel();
            log.debug("Worker thread : {}",settings.workerThreadSize);
            log.debug("Logic thread:{}",settings.cmdThreadSize);
        } catch (Exception e) {
            throw new RuntimeException("RpcClientSocketEngine start error:address="+((TCPClientSettings) settings).host+":"+settings.port);
        }
        log.info("ClientSocketEngine connect to {} success!",((TCPClientSettings) settings).host+":"+settings.port);
    }
    public Channel getChannel(){
        return this.channel;
    }

    public ISession getSession(){
        return channel.attr(Session.SESSION_KEY).get();
    }
}
