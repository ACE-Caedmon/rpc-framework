package common.server;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.annotation.RpcRequest;
import com.xl.rpc.message.LoginProtoBuffer;
import common.UserInfo;
import common.client.Command;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@RpcControl("test")
public interface IServerControl {
    @RpcMethod(Command.ServerControl_login)
    LoginProtoBuffer.Login.Builder login(@RpcRequest LoginProtoBuffer.Login.Builder login);

    @RpcMethod(Command.ServerControl_login)
    Map<String,UserInfo> login(@RpcRequest String userName,@RpcRequest String password);
    @RpcMethod(Command.ServerControl_login)
    Integer testLong(@RpcRequest Long userId);
}
