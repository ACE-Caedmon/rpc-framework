package common.client;

import com.xl.annotation.CmdMethod;
import com.xl.annotation.CmdRequest;
import com.xl.annotation.CmdUser;
import com.xl.session.ISession;
import common.UserInfo;

/**
 * Created by Caedmon on 2015/7/13.
 */
public class ClientControl {

    @CmdMethod(cmd =Command.ServerControl_login)
    public void loginSuccess(@CmdUser ISession session, @CmdRequest UserInfo userInfo){

    }

}
