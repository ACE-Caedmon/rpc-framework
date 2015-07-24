package common.server;

import com.xl.annotation.CmdControl;
import com.xl.annotation.CmdMethod;
import com.xl.annotation.CmdRequest;
import com.xl.annotation.CmdResponse;
import common.UserInfo;
import common.client.Command;

/**
 * Created by Administrator on 2015/7/20.
 */
@CmdControl("logic")
public interface IServerControl {
    @CmdMethod(cmd = Command.ServerControl_login)
    @CmdResponse
    void login(@CmdRequest String name,@CmdRequest String password);
}
