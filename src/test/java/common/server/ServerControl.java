package common.server;

import com.xl.annotation.*;
import com.xl.session.ISession;
import common.UserInfo;
import common.client.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import common.UserService;

/**
 * Created by Caedmon on 2015/7/13.
 */

@Component
public class ServerControl implements IServerControl{
    private static final Logger log= LoggerFactory.getLogger(ServerControl.class);
    @Autowired
    private UserService userService;

    @Override
    public void login(@CmdRequest String name, @CmdRequest String password) {
        log.info("接受:" + name + "," + password);
        UserInfo userInfo=new UserInfo();
        userInfo.setUserId(1);
        userInfo.setUsername("name");
        userInfo.setQq("123456");
        userInfo.setPassword(password.toString());
    }
}
