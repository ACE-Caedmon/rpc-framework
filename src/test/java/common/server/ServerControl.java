package common.server;

import com.xl.rpc.annotation.RpcRequest;
import com.xl.rpc.annotation.RpcSession;
import message.LoginProtoBuffer;
import com.xl.session.ISession;
import common.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Caedmon on 2015/7/13.
 */

@Component("serverControl")
public class ServerControl implements IServerControl{
    private static final Logger log= LoggerFactory.getLogger(ServerControl.class);

    @Override
    public LoginProtoBuffer.Login.Builder testProtobuf(LoginProtoBuffer.Login.Builder login) {
        log.info("Test_Protobuf:{}",login.toString());
        return login;
    }

    @Override
    public Name testEnum(Name name) {
        log.info("Test_Enum:"+name);
        return name;
    }

    @Override
    public List<UserInfo> testList(List<Integer> params) {
        List<UserInfo> list=new ArrayList<>();
        for(Integer param:params){
            UserInfo userInfo=new UserInfo();
            userInfo.setUsername("Name-"+param);
            list.add(userInfo);
            log.info("Test_List:{}",param);
        }
        return list;
    }

    @Override
    public Map<Integer, UserInfo> testMap(Map<Integer, UserInfo> params) {
        log.info("Test_Map:{}",params.toString());
        return params;
    }

    @Override
    public String testNull(String param) {
        log.info("Test_Null:{}",param);
        return null;
    }

    @Override
    public long testPrimitive(Long param) {
        log.info("Test_Primitive:{}",param);
        return Long.valueOf(param);
    }

    @Override
    public void testRpcSession(ISession session,String param) {
        log.info("Test_RpcSession:session={},param={}",session.getChannel(),param);
    }

    @Override
    public void testThrowable(){
        throw new NullPointerException("Exception for test");
    }

    @Override
    public UserInfo testMultipleParam(String userName, String password) {
        UserInfo userInfo=new UserInfo();
        userInfo.setUsername(userName);
        userInfo.setPassword(password);
        log.info("Test_MultipleParam:{},{}",userName,password);
        return userInfo;
    }

    @Override
    public String testNoParam() {
        return "success";
    }
}
