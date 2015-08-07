package common.client;

import com.xl.rpc.annotation.RpcMethod;
import com.xl.rpc.annotation.RpcRequest;
import com.xl.rpc.annotation.RpcSession;
import com.xl.session.ISession;
import common.UserInfo;

/**
 * Created by Caedmon on 2015/7/13.
 */
public class ClientControl {

    @RpcMethod(Command.ServerControl_login)
    public void loginSuccess(@RpcSession ISession session, @RpcRequest UserInfo userInfo){

    }

}
