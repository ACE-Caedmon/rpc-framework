package common.client;

import com.xl.annotation.RpcMethod;
import com.xl.annotation.RpcRequest;
import com.xl.annotation.RpcSession;
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
