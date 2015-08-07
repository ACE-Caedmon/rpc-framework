package javassit;

import com.xl.rpc.dispatch.method.RpcMethodDispatcher;
import com.xl.rpc.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.rpc.internal.PrototypeBeanAccess;
import common.server.ServerControl;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Administrator on 2015/7/10.
 */
public class ControlMethodTest {
    private static RpcMethodDispatcher rpcMethodDispatcher;
    @Before
    public void initControlProxyFactory() throws Exception{
        rpcMethodDispatcher =new JavassitRpcMethodDispatcher(new PrototypeBeanAccess(),10);
    }
    public void createMethodProxy(Class controlClass) throws Exception{
        String basePackage="common.server";
        rpcMethodDispatcher.loadClasses(controlClass);
    }
    @Test
    public void createProxyControl() throws Exception{
        createMethodProxy(ServerControl.class);
    }
    @Test
    public void getProxyControl() throws Exception{

    }
}
