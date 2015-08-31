package rpc;

import message.LoginProtoBuffer;
import common.Command;
import common.UserInfo;
import common.server.Name;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Caedmon on 2015/7/17.
 */
public class RemoteCallTest extends BasicTest{
    @Test
    public void testProtobuf(){
        LoginProtoBuffer.Login.Builder message= LoginProtoBuffer.Login.newBuilder();
        message.setUsername("Test_Proto_Name");
        message.setPassword("Test_Proto_Password");
        LoginProtoBuffer.Login.Builder result=serverControl.testProtobuf(message);
        boolean nameOK=message.getUsername().equals(result.getUsername());
        boolean passwordOk=message.getPassword().equals(result.getPassword());
        Assert.assertEquals(true,nameOK&&passwordOk);
    }
    @Test
    public void testEnum(){
        Name param=Name.Caedmon;
        Name result=serverControl.testEnum(param);
        Assert.assertEquals(param,result);
    }
    @Test
    public void testList(){

        List<Integer> params=new ArrayList<>();
        for(int i=0;i<10;i++){
            params.add(i);
        }
        long start=System.currentTimeMillis();
        List<UserInfo> result=serverControl.testList(params);
        long end=System.currentTimeMillis();
        Assert.assertEquals(params.size(),result.size());
    }
    @Test
    public void testMap(){
        Map<Integer,UserInfo> params=new HashMap<>();
        params.put(1,new UserInfo());
        Map<Integer,UserInfo> result=serverControl.testMap(params);
        Assert.assertEquals(params.size(),result.size());
    }
    @Test
    public void testNull(){
        String param=null;
        String result=serverControl.testNull(param);
        Assert.assertEquals(null,result);
    }
    @Test
    public void testPrimitive(){
        int param=100;
        long result=serverControl.testPrimitive(param);
        Assert.assertEquals(param,result);
    }
    @Test
    public void testRpcSession(){
        container.getRpcClientApi().asyncRpcCall("test", Command.Test_RpcSession, new Class[]{String.class}, "Test_RpcSession");
    }
    @Test
    public void testThrowable(){
        serverControl.testThrowable();
    }
    @Test
    public void testMultipleParam(){
        final String name="name";
        final String password="password";
        UserInfo userInfo=serverControl.testMultipleParam(name, password);
        Assert.assertEquals(name,userInfo.getUsername());
        Assert.assertEquals(password, userInfo.getPassword());
    }
    @Test
    public void testNoParam(){
        Assert.assertEquals("success",serverControl.testNoParam());
    }
}
