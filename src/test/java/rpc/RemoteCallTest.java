package rpc;

import com.xl.rpc.monitor.MonitorInformation;
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
    public void testProtobuf() throws Exception{
        LoginProtoBuffer.Login.Builder message= LoginProtoBuffer.Login.newBuilder();
        message.setUsername("Test_Proto_Name");
        message.setPassword("Test_Proto_Password");
        LoginProtoBuffer.Login.Builder result= syncServerControl.testProtobuf(message);
        boolean nameOK=message.getUsername().equals(result.getUsername());
        boolean passwordOk=message.getPassword().equals(result.getPassword());
        Assert.assertEquals(true,nameOK&&passwordOk);
        Thread.sleep(1000000);
    }
    @Test
    public void testEnum(){
        Name param=Name.Caedmon;
        Name result= syncServerControl.testEnum(param);
        Assert.assertEquals(param,result);
    }
    @Test
    public void testList(){

        List<Integer> params=new ArrayList<>();
        for(int i=0;i<10;i++){
            params.add(i);
        }
        long start=System.currentTimeMillis();
        List<UserInfo> result= syncServerControl.testList(params);
        long end=System.currentTimeMillis();
        Assert.assertEquals(params.size(),result.size());
    }
    @Test
    public void testMap(){
        Map<Integer,UserInfo> params=new HashMap<>();
        params.put(1,new UserInfo());
        Map<Integer,UserInfo> result= syncServerControl.testMap(params);
        Assert.assertEquals(params.size(),result.size());
    }
    @Test
    public void testNull(){
        String param=null;
        String result= syncServerControl.testNull(param);
        Assert.assertEquals(null,result);
    }
    @Test
    public void testPrimitive(){
        long param=100;
        long result= syncServerControl.testPrimitive(param);
        Assert.assertEquals(param,result);
    }
    @Test
    public void testRpcSession() throws Exception{
        container.getRpcClientApi().asyncRpcCall("test", "192.168.1.9:8003", Command.Test_RpcSession, "Test_RpcSession");
    }
    @Test
    public void testThrowable(){
        asyncServerControl.testThrowable();

        //syncServerControl.testThrowable();
    }
    @Test
    public void testMultipleParam(){
        final String name="name";
        final String password="password";
        UserInfo userInfo= syncServerControl.testMultipleParam(name, password);
        Assert.assertEquals(name,userInfo.getUsername());
        Assert.assertEquals(password, userInfo.getPassword());
    }
    @Test
    public void testNoParam(){
        Assert.assertEquals("success", syncServerControl.testNoParam());
    }

    @Test
    public void testMonitor(){
    }
}
