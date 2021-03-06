package com.xiaoluo.rpc.boot;

import com.xiaoluo.rpc.dispatch.method.RpcMethodDispatcher;
import com.xiaoluo.rpc.dispatch.method.ReflectRpcMethodDispatcher;
import com.xiaoluo.rpc.internal.PrototypeBeanAccess;
import com.xiaoluo.rpc.dispatch.tcp.TCPServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/4/25.
 */
public class ServerSocketEngine extends SocketEngine{
    private static final Logger log= LoggerFactory.getLogger(ServerSocketEngine.class);
    private ServerSettings settings;
    public ServerSocketEngine(ServerSettings settings,RpcMethodDispatcher rpcMethodDispatcher) {
        super(settings, rpcMethodDispatcher);
        this.settings=settings;
    }
    public ServerSocketEngine(ServerSettings settings){
        this(settings,new ReflectRpcMethodDispatcher(new PrototypeBeanAccess(),Runtime.getRuntime().availableProcessors()));
    }
    /**
     * 启动网络服务
     * */
    public void startSocket() throws Exception{
        final EventLoopGroup bossGroup = new NioEventLoopGroup(settings.bossThreadSize);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(settings.workerThreadSize);
        try {
            ChannelInitializer<SocketChannel> initializer=null;
            switch (settings.protocol.toLowerCase()){
                case TCP_PROTOCOL:
                    initializer=new TCPServerInitializer(rpcMethodDispatcher);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupport protocol:protocol = "+settings.protocol);
            }

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(initializer);
            ChannelFuture f =  b.bind(settings.port).sync();
            log.info("Boss thread : {}",settings.bossThreadSize);
            log.info("Worker thread : {}", settings.workerThreadSize);
            log.info("Logic thread:{}", settings.cmdThreadSize);
            log.info("Socket port :{}", settings.port);

            //f.channel().closeFuture().sync();
        } catch (Exception e) {
            if(log.isErrorEnabled()){
                log.error("ServerSocketEngine Start Error:port={}",this.settings.port,e);
            }
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            throw  e;
        }
        log.info("ServerSocketEngine Start OK!");
    }
}
