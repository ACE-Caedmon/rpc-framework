/**
 * Channel初始化的自定义类，用来定义解码编码以及相关事件处理器
 * @author Chenlong
 * */
package com.xl.rpc.dispatch.tcp;


import com.xl.rpc.codec.rpc.RpcDecoder;
import com.xl.rpc.codec.rpc.RpcEncoder;
import com.xl.rpc.codec.rpc.RpcServerMarkEncoder;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.utils.EngineParams;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

@Sharable
public class TCPServerInitializer extends ChannelInitializer<SocketChannel>{
    private RpcMethodDispatcher controlProxyDispatcher;
	public TCPServerInitializer(RpcMethodDispatcher rpcMethodDispatcher){
		this.controlProxyDispatcher = rpcMethodDispatcher;
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
		ch.pipeline().addLast(new RpcServerMarkEncoder());
		ch.pipeline().addLast(new TCPInboundHandler(controlProxyDispatcher));


	}

}
