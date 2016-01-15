package com.xl.rpc.codec.rpc;

import com.xl.rpc.annotation.MsgType;
import com.xl.rpc.boot.EngineSettings;
import com.xl.rpc.codec.*;
import com.xl.rpc.exception.VersionMisMatchException;
import com.xl.rpc.monitor.MonitorConstant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
 public class RpcDecoder extends MessageToMessageDecoder<BinaryPacket> {
	private static final Logger log =LoggerFactory.getLogger(RpcDecoder.class);
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
        //版本号
        float version=buffer.readFloat();
        if(version!= EngineSettings.VERSION){
            throw new VersionMisMatchException("The sender version is "+version+", the receiver is "+EngineSettings.VERSION+","+ctx.channel());
        }
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
        int paramsLength=buffer.readInt();
        //解码
        List<Object> params=new ArrayList<>(paramsLength);
        List<String> classNameList=new ArrayList<>(paramsLength);
        for(int i=0;i<paramsLength;i++){
            boolean isNull=buffer.readBoolean();
            if(!isNull){
                String className=buffer.readString();
                Class clazz=Class.forName(className);
                Object param=CodecKit.decode(msgType, clazz, buffer);
                classNameList.add(className);
                params.add(param);
            }else{
                classNameList.add(null);
                params.add(null);
            }
        }

        RpcPacket rpcPacket =new RpcPacket(cmd,params.toArray());
        rpcPacket.setFromCall(fromCall);
        rpcPacket.setSync(sync);
        rpcPacket.setUuid(uuid);
        rpcPacket.setException(isException);
        String[] classNameArray=new String[classNameList.size()];
        classNameList.toArray(classNameArray);
        rpcPacket.setClassNameArray(classNameArray);
        if(!cmd.equals(MonitorConstant.MonitorServerMethod.HEART_BEAT)){
            log.debug("Rpc decode {}:{}", ctx.channel(), rpcPacket.toString());
        }
        out.add(rpcPacket);
    }

     @Override
     public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         super.exceptionCaught(ctx, cause);
         log.error("Exception Caught {}",ctx.channel(),cause);
     }
 }
