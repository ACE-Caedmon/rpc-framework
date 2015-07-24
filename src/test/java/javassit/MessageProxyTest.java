package javassit;

import com.xl.annotation.MsgType;
import com.xl.codec.DataBuffer;
import com.xl.dispatch.message.MessageProxy;
import com.xl.dispatch.message.MessageProxyFactory;
import common.UserInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Caedmon on 2015/7/12.
 */
public class MessageProxyTest {
    private static MessageProxyFactory holder;
    @Before
    public void init(){
        holder=MessageProxyFactory.ONLY_INSTANCE;
    }
    public MessageProxy createUserProxy() throws Exception{
        return holder.getMessageProxy(MsgType.JSON, UserInfo.class);
    }
    @Test
    public void createMessageProxy() throws Exception{
        MessageProxy proxy=createUserProxy();
        final String username="testname";
        UserInfo encodeUserInfo =new UserInfo();
        encodeUserInfo.setUsername(username);
        DataBuffer buffer=proxy.encode(encodeUserInfo);
        UserInfo decodeUserInfo =(UserInfo)proxy.decode(buffer);
        Assert.assertEquals(encodeUserInfo.getUsername(), decodeUserInfo.getUsername());
    }
    @Test
    public void getMessageProxy() throws Exception{
        MessageProxy proxy=createUserProxy();
        Assert.assertEquals(proxy,holder.getMessageProxy(MsgType.JSON, UserInfo.class));
    }
}
