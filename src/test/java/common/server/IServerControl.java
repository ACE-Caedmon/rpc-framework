package common.server;

import com.xl.annotation.CmdControl;
import com.xl.annotation.CmdMethod;
import com.xl.annotation.CmdRequest;
import com.xl.annotation.CmdResponse;
import common.UserInfo;
import common.client.Command;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/7/20.
 */
@CmdControl("account")
public interface IServerControl {
    @CmdMethod(cmd = Command.ServerControl_login)
    @CmdResponse
    Map<String,UserInfo> login(@CmdRequest String name,@CmdRequest String password);
    @CmdMethod(cmd = Command.ServerControl_getUserInfo)
    @CmdResponse
    Map<String,Integer> getUserInfo(@CmdRequest String name,@CmdRequest String password);
}
