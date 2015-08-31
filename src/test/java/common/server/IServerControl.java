package common.server;

import com.xl.rpc.annotation.RpcControl;
import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.annotation.RpcRequest;
import com.xl.rpc.annotation.RpcSession;
import message.LoginProtoBuffer;
import com.xl.session.ISession;
import common.Command;
import common.UserInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@RpcControl("test")
public interface IServerControl {
    @RpcMethod(Command.Test_Protobuf)
    LoginProtoBuffer.Login.Builder testProtobuf(@RpcRequest LoginProtoBuffer.Login.Builder login);
    @RpcMethod(Command.Test_Enum)
    Name testEnum(@RpcRequest Name name);
    @RpcMethod(Command.Test_List)
    List<UserInfo> testList(@RpcRequest List<Integer> params);
    @RpcMethod(Command.Test_Map)
    Map<Integer,UserInfo> testMap(@RpcRequest Map<Integer,UserInfo> params);
    @RpcMethod(Command.Test_Null)
    String testNull(@RpcRequest String param);
    @RpcMethod(Command.Test_Primitive)
    long testPrimitive(@RpcRequest int param);
    @RpcMethod(Command.Test_RpcSession)
    void testRpcSession(@RpcSession ISession session,@RpcRequest String param);
    @RpcMethod(Command.Test_Throwable)
    void testThrowable();
    @RpcMethod(Command.Test_MultipleParam)
    UserInfo testMultipleParam(@RpcRequest String userName,@RpcRequest String password);
    @RpcMethod(Command.Test_NoParam)
    String testNoParam();

}
