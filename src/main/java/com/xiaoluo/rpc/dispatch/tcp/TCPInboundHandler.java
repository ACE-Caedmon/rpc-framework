/**
 * @author Chenlong
 * 确保MesageDispatcher是Sharable的才能正确运行，不能每次Channel创建New一个新的
 * 该类负责一些系统事件处理，包括将Netty层消息转到自定义框架层进行处理
 * */
package com.xiaoluo.rpc.dispatch.tcp;

import com.xiaoluo.rpc.codec.RpcPacket;
import com.xiaoluo.rpc.dispatch.method.RpcCallback;
import com.xiaoluo.rpc.dispatch.method.RpcMethodDispatcher;
import com.xiaoluo.rpc.dispatch.method.SyncRpcCallBack;
import com.xiaoluo.rpc.dispatch.ISession;
import com.xiaoluo.rpc.dispatch.Session;
import com.xiaoluo.rpc.dispatch.SessionFire;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelProgressivePromise;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@Sharable
public class TCPInboundHandler extends SimpleChannelInboundHandler<RpcPacket>{
	private static final Logger log = LoggerFactory.getLogger(TCPInboundHandler.class);
	private RpcMethodDispatcher rpcMethodDispatcher;
	public TCPInboundHandler(RpcMethodDispatcher rpcMethodDispatcher){
		this(rpcMethodDispatcher,null);
	}
	public TCPInboundHandler(RpcMethodDispatcher rpcMethodDispatcher, DefaultChannelProgressivePromise activePromise){
		this.rpcMethodDispatcher=rpcMethodDispatcher;
	}
    /**
     * 连接断开是会调用此方法，方法会将Session相关信息移除，并且从Channel删除保存的Session对象
     * 并且会触发应用层扩展的离线时间
     * @see SessionFire
     * */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final ISession session=ctx.channel().attr(Session.SESSION_KEY).get();
		SessionFire.getInstance().fireEvent(SessionFire.SessionEvent.SESSION_DISCONNECT, session);
		session.clear();
		log.debug("Channel inActive:{}",ctx.channel());

	}
    /**
     * 有客户端数据发送到服务端时会调用此方法，提交给自定义线程池处理业务逻辑。
     * @param  ctx
     * */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx,final RpcPacket rpcPacket)
			throws Exception {
		final ISession session=ctx.channel().attr(Session.SESSION_KEY).get();
		rpcMethodDispatcher.dispatch(session,rpcPacket);
	}
    /**
     * 连接创建时会调用此方法，此时会负责创建ISession,并且为ISession分配一个Actor
     * */
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		ISession session=ctx.channel().attr(Session.SESSION_KEY).get();
		SessionFire.getInstance().fireEvent(SessionFire.SessionEvent.SESSION_CONNECT, session);
		log.debug("ChannelActive:{}",ctx.channel().toString());
	}
    /**
     * 出现异常时
     * */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		final ISession session=new Session(ctx.channel());
		ctx.channel().attr(Session.SESSION_KEY).set(session);
		session.setAttribute(Session.SYNC_CALLBACK_MAP, new HashMap<String, SyncRpcCallBack<?>>());
		session.setAttribute(Session.ASYNC_CALLBACK_MAP, new HashMap<String, RpcCallback>());
	}
}
