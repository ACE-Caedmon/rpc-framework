package common.server;

import com.xl.annotation.RpcControl;
import com.xl.annotation.RpcMethod;
import com.xl.annotation.RpcRequest;
import com.xl.annotation.RpcResponse;
import com.xl.message.LoginProtoBuffer;
import common.UserInfo;
import common.client.Command;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@RpcControl("test")
public interface IServerControl {
    @RpcMethod(Command.ServerControl_login)
    @RpcResponse
    Map<String,UserInfo> login(@RpcRequest LoginProtoBuffer.Login.Builder login);

    @RpcMethod(Command.ServerControl_login)
    @RpcResponse
    Map<String,UserInfo> login(@RpcRequest String userName,@RpcRequest String password);
}
