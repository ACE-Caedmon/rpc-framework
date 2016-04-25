package common.server;

import com.xiaoluo.rpc.annotation.RpcControl;
import com.xiaoluo.rpc.annotation.RpcMethod;
import com.xiaoluo.rpc.dispatch.ISession;
import common.Command;
import common.UserInfo;
import message.LoginProtoBuffer;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@RpcControl("test")
public interface IServerControl {
    @RpcMethod(Command.Test_Protobuf)
    LoginProtoBuffer.Login.Builder testProtobuf(LoginProtoBuffer.Login.Builder login);
    @RpcMethod(Command.Test_Enum)
    Name testEnum(Name name);
    @RpcMethod(Command.Test_List)
    List<UserInfo> testList(List<Integer> params);
    @RpcMethod(Command.Test_Map)
    Map<Integer,UserInfo> testMap(Map<Integer,UserInfo> params);
    @RpcMethod(Command.Test_Null)
    String testNull(String param);
    @RpcMethod(Command.Test_Primitive)
    long testPrimitive(Long param);
    @RpcMethod(Command.Test_RpcSession)
    void testRpcSession(ISession session,String param);
    @RpcMethod(Command.Test_Throwable)
    void testThrowable();
    @RpcMethod(Command.Test_MultipleParam)
    UserInfo testMultipleParam(String userName,String password);
    @RpcMethod(Command.Test_NoParam)
    String testNoParam();

}
