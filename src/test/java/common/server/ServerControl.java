package common.server;

import com.xl.annotation.RpcRequest;
import com.xl.message.LoginProtoBuffer;
import common.UserInfo;
import common.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Caedmon on 2015/7/13.
 */

@Component("serverControl")
public class ServerControl implements IServerControl{
    private static final Logger log= LoggerFactory.getLogger(ServerControl.class);
    @Autowired
    private UserService userService;

    @Override
    public Map<String,UserInfo> login(@RpcRequest LoginProtoBuffer.Login.Builder login) {
        log.info("Protobuf接受:{},{}",login.getUsername(), login.getPassword());
        Map<String,UserInfo> userInfos=new HashMap<>();
        return userInfos;
    }

    @Override
    public Map<String, UserInfo> login(@RpcRequest String userName, @RpcRequest String password) {
        log.info("JSON接受:{},{}",userName,password);
        Map<String,UserInfo> userInfos=new HashMap<>();
        for(int i=0;i<100;i++){
            UserInfo userInfo=new UserInfo();
            userInfo.setUserId(i);
            userInfo.setUsername("user-"+i);
            userInfos.put(userInfo.getUsername(),userInfo);
        }
        return userInfos;
    }
}
