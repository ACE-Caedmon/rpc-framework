package common.server;

import com.xl.rpc.annotation.RpcRequest;
import com.xl.rpc.message.LoginProtoBuffer;
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
    public LoginProtoBuffer.Login.Builder login(@RpcRequest LoginProtoBuffer.Login.Builder login) {
        log.info("Protobuf接受:{},{}",login.getUsername(), login.getPassword());
        return login;
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
        return null;
    }

    @Override
    public Integer testLong(@RpcRequest Long userId) {
        return 100000;
    }
    public static void main(String[] args) {
        System.out.println(new Long(1L).toString());
        System.out.println(new Integer(1).toString());
        System.out.println(new Character('C').toString());
        System.out.println(new Byte((byte)1).toString());
        System.out.println(new Float(1.1).toString());
        System.out.println(new Short((short)1).toString());


    }
}
