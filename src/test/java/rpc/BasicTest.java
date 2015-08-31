package rpc;

import com.xl.rpc.internal.InternalContainer;
import common.server.IServerControl;
import org.junit.Before;

/**
 * Created by Administrator on 2015/8/26.
 */
public class BasicTest {
    protected static final InternalContainer container=InternalContainer.getInstance();
    protected static IServerControl syncServerControl;
    protected static IServerControl asyncServerControl;
    @Before
    public void init(){
        InternalContainer container=InternalContainer.getInstance();
        container.startRpcServer("rpc.properties");
        container.startRpcClient("rpc.properties");
        syncServerControl =container.getSyncRemoteCallProxy(IServerControl.class);
        asyncServerControl=container.getAsyncRemoteCallProxy(IServerControl.class);
    }
}
