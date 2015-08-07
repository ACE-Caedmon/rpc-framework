package com.xl.rpc.codec.rpc;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.codec.DefaultPracticalBuffer;
import com.xl.rpc.codec.PracticalBuffer;
import com.xl.rpc.codec.RpcPacket;
import com.xl.rpc.codec.BinaryPacket;
import com.xl.rpc.dispatch.method.ControlMethod;
import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.dispatch.message.MessageProxy;
import com.xl.rpc.dispatch.message.MessageProxyFactory;
import com.xl.session.Session;
import com.xl.utils.CommonUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
 public class RpcDecoder extends MessageToMessageDecoder<BinaryPacket> {
	private static final Logger log =LoggerFactory.getLogger(RpcDecoder.class);
    private RpcMethodDispatcher rpcMethodDispatcher;
    public RpcDecoder(RpcMethodDispatcher rpcMethodDispatcher){
        this.rpcMethodDispatcher = rpcMethodDispatcher;
    }
	/**
     * 负责对数据包进行解码
	 * @param ctx 对应Channel的上下文
	 * @param packet 数据包
	 * @param out 输出事件对象
	 * */
	@Override
	protected void decode(ChannelHandlerContext ctx, BinaryPacket packet,
			List<Object> out) throws Exception {
        PracticalBuffer buffer=new DefaultPracticalBuffer(packet.getContent());
        //命令ID
        String cmd=buffer.readString();
        //是否来自客户端
        boolean fromCall=buffer.readBoolean();
        //是否为同步消息
        boolean sync=buffer.readBoolean();
        //获取消息中的UUID
        String uuid=buffer.readString();
        //是否为异常消息
        boolean isException=buffer.readBoolean();
        //消息类型
        MsgType msgType=MsgType.valueOf(buffer.readInt());
        //类名
        String[] classNameArray=buffer.readString().split(",");
        //解码
        List<Object> content=new ArrayList<>();
        for(String className:classNameArray){
            if(className==null||className.equals("null")){
                content.add(null);
                continue;
            }
            Object param=null;
            Class clazz=Class.forName(className);
            if(Throwable.class.isAssignableFrom(clazz)){
                int exceptionLen=buffer.readInt();
                byte[] bytes=new byte[exceptionLen];
                buffer.getByteBuf().readBytes(bytes);
                param=CommonUtils.derialize(bytes,Throwable.class);
            }else{
                MessageProxy messageProxy= MessageProxyFactory.ONLY_INSTANCE.getMessageProxy(msgType, clazz);
                if(messageProxy!=null){
                    param=messageProxy.decode(buffer);
                }
            }
            content.add(param);

        }
        RpcPacket rpcPacket =new RpcPacket(cmd,content.toArray());
        rpcPacket.setFromCall(fromCall);
        rpcPacket.setSync(sync);
        rpcPacket.setUuid(uuid);
        rpcPacket.setException(isException);
        rpcPacket.setClassNameArray(classNameArray);
        log.debug("Rpc decode :packet = {},time = {}",rpcPacket.toString(),System.currentTimeMillis());
        try {
            ControlMethod methodProxy= rpcMethodDispatcher.newControlMethodProxy(rpcPacket);
            if(methodProxy!=null){
                out.add(methodProxy);
            }
        }catch (Exception e){
            e.printStackTrace();
            rpcPacket.setException(true);
            rpcPacket.setParams(e);
            ctx.channel().attr(Session.SESSION_KEY).get().writeAndFlush(rpcPacket);
        }

    }
}
