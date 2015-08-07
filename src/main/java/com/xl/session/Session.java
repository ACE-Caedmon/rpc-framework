package com.xl.session;


/**
 * @author Chenlong
 * 客户端连接对应的Session
 * */

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.dispatch.SocketPacket;
import com.xl.rpc.dispatch.method.RpcCallback;
import com.xl.rpc.dispatch.method.SyncRpcCallBack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Session implements ISession{
	/**Session对象Key**/
	public static final AttributeKey<ISession> SESSION_KEY=new AttributeKey<ISession>("SESSION_KEY");
	/**秘钥*/
	public static final AttributeKey<String> SECRRET_KEY=new AttributeKey<String>("SECRRET_KEY");
	/**发送给客户端的数据是否需要加密**/
	public static final AttributeKey<Boolean> NEED_ENCRYPT=new AttributeKey<>("NEED_ENCRYPT");
	/**每个客户端连接存储的密码表**/
	public static final AttributeKey<List<Short>>  PASSPORT=new AttributeKey<>("PASSPORT");
	/**同步返回Future*/
	public static final AttributeKey<Map<String,SyncRpcCallBack<?>>> SYNC_CALLBACK_MAP =new AttributeKey<>("SYNC_CALLBACK_MAP");
	/**异步回调Map*/
	public static final AttributeKey<Map<String,RpcCallback>> ASYNC_CALLBACK_MAP=new AttributeKey<>("ASYNC_CALLBACK_MAP");
	private Channel channel;//连接通道
	private long createTime;//创建时间
	private long lastActiveTime;//最后活动时间
	private volatile boolean active=true;
	//Netty已经触发过inActive
	private AtomicBoolean nettyInActive=new AtomicBoolean(false);
    private static Logger logger= LoggerFactory.getLogger(Session.class);
	public Session(Channel channel){
		this.channel=channel;
		this.createTime=System.currentTimeMillis();
	}
	@Override
	public long getCreateTime() {
		// TODO Auto-generated method stub
		return this.createTime;
	}
	@Override
	public Channel getChannel() {
		return channel;
	}
	@Override
	public String getClientAddress() {
		// TODO Auto-generated method stub
		return channel.remoteAddress().toString();
	}
	@Override
	public int getClientPort() {
		// TODO Auto-generated method stub
		return Integer.valueOf(channel.remoteAddress().toString().split(":")[1]).intValue();
	}
	@Override
	public Future<?> disconnect(boolean immediately) {
		ChannelFuture future=null;
		try{
			if(!nettyInActive.get()){
				nettyInActive.compareAndSet(false,true);
				future=channel.disconnect();
				if(immediately){
					future.sync();
				}
				SessionFire.getInstance().fireEvent(SessionFire.SessionEvent.SESSION_DISCONNECT,Session.this);
			}
		}catch (Exception e){
			e.printStackTrace();
			logger.error("Disconnect error",e);
		}

		return future;
	}
	@Override
	public long getLastActiveTime() {
		// TODO Auto-generated method stub
		return lastActiveTime;
	}
	@Override
	public void setLastActiveTime(long lastActiveTime) {
		// TODO Auto-generated method stub
		this.lastActiveTime=lastActiveTime;
	}
	public Future<?> writeAndFlush(SocketPacket packet) {
		if(channel.isActive()){
			final ChannelFuture future=channel.writeAndFlush(packet);
			setLastActiveTime(System.currentTimeMillis());
			return future;
		}else{
			logger.error("Session is not active:packet = {}",packet.toString());
			return channel.newSucceededFuture();
		}
	}
	@Override
	public boolean containsAttribute(AttributeKey<?> key) {
		// TODO Auto-generated method stub
		return channel.attr(key).get()!=null;
	}
	@Override
	public <T> T getAttribute(AttributeKey<T> key) {
		// TODO Auto-generated method stub
		return channel.attr(key).get();
	}
	@Override
	public void clear() {

	}
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return channel.isActive()&&active;
	}
	@Override
	public void setActive(boolean active) {
		// TODO Auto-generated method stub
		this.active=active;
	}
	@Override
	public Future<?> disconnect(boolean immediately, SocketPacket packet) {
		Future<?> future=channel.newSucceededFuture();
		if(channel.isActive()){
			future=this.writeAndFlush(packet);
			try {
				if(immediately){
					future.sync();
				}
				future=disconnect(immediately);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
                logger.error("Wait disconnect task error",e);
			}
		}
		return future;
	}
	@Override
	public <T> void removeAttribute(AttributeKey<T> key) {
		channel.attr(key).remove();
	}

	@Override
	public <T> void setAttribute(AttributeKey<T> key, T value) {
		channel.attr(key).set(value);
	}

	@Override
	public <T> T syncRpcSend(RpcPacket packet,Class<T> resultType,long timeout,TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		packet.setUuid(UUID.randomUUID().toString());
		String uuid=packet.getUuid();
		try {
			DefaultProgressivePromise<T> syncPromise=new DefaultProgressivePromise<>(channel.eventLoop());
			SyncRpcCallBack callBack=new SyncRpcCallBack(syncPromise);
			Map<String,SyncRpcCallBack<?>> callBackMap=getAttribute(SYNC_CALLBACK_MAP);
			callBackMap.put(uuid, callBack);
			writeAndFlush(packet);
			T result=syncPromise.get(timeout, TimeUnit.SECONDS);
			callBackMap.remove(uuid);
			return result;
		}finally {
			getAttribute(SYNC_CALLBACK_MAP).remove(uuid);
		}
	}
	public void asyncRpcSend(RpcPacket packet,RpcCallback rpcCallback){
		String uuid=UUID.randomUUID().toString();
		packet.setUuid(uuid);
		if(rpcCallback!=null){
			Map<String,RpcCallback> callBackMap=getAttribute(ASYNC_CALLBACK_MAP);
			callBackMap.put(uuid, rpcCallback);
		}
		writeAndFlush(packet);
	}

}
