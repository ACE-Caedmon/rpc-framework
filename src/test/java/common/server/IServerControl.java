package common.server;

import com.xl.annotation.RpcControl;
import com.xl.annotation.RpcMethod;
import com.xl.annotation.RpcRequest;
import com.xl.annotation.RpcResponse;
import common.UserInfo;
import common.client.Command;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@RpcControl("test")
public interface IServerControl {
    @RpcMethod(cmd = Command.ServerControl_login)
    @RpcResponse
    Map<String,UserInfo> login(@RpcRequest String name,@RpcRequest String password);
    @RpcMethod(cmd = Command.ServerControl_getUserInfo)
    @RpcResponse
    Map<String,Integer> getUserInfo(@RpcRequest String name,@RpcRequest String password);
}
