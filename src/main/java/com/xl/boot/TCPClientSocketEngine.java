package com.xl.boot;

import com.xl.dispatch.tcp.TCPClientInitializer;
import com.xl.dispatch.handler.ValidateOKHandler;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.session.ISession;
import com.xl.session.Session;
import com.xl.session.SessionFire;
import com.xl.utils.NGSocketParams;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultProgressivePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/4/25.
 */
public class TCPClientSocketEngine extends SocketEngine{
    private static final Logger log= LoggerFactory.getLogger(TCPClientSocketEngine.class);
    private EventLoopGroup eventExecutors;
    private Channel channel;
    public TCPClientSocketEngine(TCPClientSettings settings,RpcMethodDispatcher rpcMethodDispatcher) {
        super(settings, rpcMethodDispatcher);
    }
    public TCPClientSocketEngine(TCPClientSettings settings,RpcMethodDispatcher rpcMethodDispatcher,EventLoopGroup eventExecutors) {
        super(settings, rpcMethodDispatcher);
        this.settings=settings;
        this.eventExecutors=eventExecutors;
    }
    @Override
    public void startSocket() {
        log.info("ClientSocketEngine Init !");
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
            log.info("Protocol type: {}",TCP_PROTOCOL);
            log.info("Worker thread : {}",settings.workerThreadSize);
            log.info("Logic thread:{}",settings.cmdThreadSize);
            log.info("Socket package encrypt : {}", NGSocketParams.isSocketPacketEncrypt());
            log.info("Cmd Dispatcher : {}", rpcMethodDispatcher.getClass().getCanonicalName());
            log.info("Socket port :{}",settings.port);

        } catch (Exception e) {
            e.printStackTrace();
            if(log.isErrorEnabled()){
                log.error("<<<<<<<SocketEngine Start Error!>>>>>>", e);
            }
            workerGroup.shutdownGracefully();
            return;
        }
        //如果系统配置不加密则不发送密码表
        if(NGSocketParams.isSocketPacketEncrypt()){
            //用来给客户端发送密码表
            SessionFire.getInstance().registerEvent(SessionFire.SessionEvent.SESSION_LOGIN, new ValidateOKHandler());
        }
        log.info("ClientSocketEngine Start OK!");
    }
    public Channel getChannel(){
        return this.channel;
    }

    public ISession getSession(){
        return channel.attr(Session.SESSION_KEY).get();
    }
}
