package com.xl.codec.rpc;

import com.xl.annotation.MsgType;
import com.xl.codec.BinaryCodecApi;
import com.xl.codec.PracticalBuffer;
import com.xl.codec.RpcPacket;
import com.xl.codec.binary.BinaryPacket;
import com.xl.dispatch.method.ControlMethod;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.dispatch.message.MessageProxy;
import com.xl.dispatch.message.MessageProxyFactory;
import com.xl.utils.CommonUtils;
import com.xl.utils.NGSocketParams;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络数据报文解码处理器
 * @author Chenlong
 * 协议格式<br>
 *     <table  border frame="box">
 *         <tr>
 *             <th style="text-align:center"></th>
 *             <th style="text-align:center">包长</th>
 *             <th style="text-align:center">是否加密</th>
 *             <th style="text-align:center">密码表索引</th>
 *             <th style="text-align:center">指令ID(cmd)</th>
 *             <th style="text-align:center">消息体(MessageHandler中自定义内容)</th>
 *         </tr>
 *         <tr>
 *             <td>数据类型</td>
 *             <td style="text-align:center">int</td>
 *             <td style="text-align:center">byte</td>
 *             <td style="text-align:center">byte</td>
 *             <td style="text-align:center">int</td>
 *             <td style="text-align:center">byte[]</td>
 *         </tr>
 *             <td>每部分字节数</td>
 *             <td style="text-align:center">4</td>
 *             <td style="text-align:center">1</td>
 *             <td style="text-align:center">1</td>
 *             <td style="text-align:center">4</td>
 *             <td style="text-align:center">根据消息体内容计算</td>
 *         </tr>
 *         <tr>
 *             <td style="text-align:center">是否加密</td>
 *             <td colspan="2" style="text-align:center">未加密部分</td>
 *             <td colspan="3" style="text-align:center">加密部分</td>
 *         </tr>
 *     </table>
 * */
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
        PracticalBuffer buffer= BinaryCodecApi.decodeBody(packet, ctx);
        boolean fromCall=buffer.readBoolean();
        //是否为同步消息
        boolean sync=buffer.readBoolean();
        //命令ID
        int cmd=buffer.readInt();
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
        ControlMethod methodProxy= rpcMethodDispatcher.newControlMethodProxy(rpcPacket);
        if(methodProxy!=null){
            out.add(methodProxy);
        }else{
            if(NGSocketParams.isWarnUnKownCmd()){
                log.warn("未注册指令:cmd = {}",cmd);
            }
        }
    }
}
