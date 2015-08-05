package interceptor;

import com.xl.codec.RpcPacket;
import common.client.Command;
import com.xl.dispatch.MethodInterceptor;
import com.xl.session.ISession;
import common.AttributeName;

/**
 * Created by Administrator on 2015/7/13.
 */
public class LoginInterceptor implements MethodInterceptor{
    @Override
    public boolean beforeDoCmd(ISession session,RpcPacket packet) {
        System.out.println("拦截器:before");
        String cmd=packet.getCmd();
        if(cmd.equals(Command.ServerControl_login)||cmd.equals(Command.ServerControl_login)){
            return true;
        }else{
            if(session.containsAttribute(AttributeName.USER_NAME)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterDoCmd(ISession session, RpcPacket packet) {
        System.out.println("拦截器:after");
    }

    @Override
    public void exceptionCaught(ISession session, RpcPacket packet, Throwable cause) {
        System.out.println("拦截器:exception");
        cause.printStackTrace();
    }
}
