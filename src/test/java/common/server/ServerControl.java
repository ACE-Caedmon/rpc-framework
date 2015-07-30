package common.server;

import com.xl.annotation.*;
import common.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import common.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Caedmon on 2015/7/13.
 */

@Component
public class ServerControl implements IServerControl{
    private static final Logger log= LoggerFactory.getLogger(ServerControl.class);
    @Autowired
    private UserService userService;

    @Override
    public Map<String,UserInfo> login(@RpcRequest String name, @RpcRequest String password) {
        log.info("接受:" + name + "," + password);
        UserInfo userInfo=new UserInfo();
        userInfo.setUserId(1);
        userInfo.setUsername("name");
        userInfo.setQq("123456");
        userInfo.setPassword(password.toString());
        Map<String,UserInfo> userInfos=new HashMap<>();
        userInfos.put(userInfo.getUsername(),userInfo);
        return userInfos;
    }

    @Override
    public Map<String, Integer> getUserInfo(@RpcRequest String name, @RpcRequest String password) {
        Map<String,Integer> map=new HashMap<>();
        map.put(name,1000);
        return map;
    }
}
