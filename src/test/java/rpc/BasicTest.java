package rpc;

import com.xl.rpc.internal.InternalContainer;
import com.xl.rpc.monitor.client.IRpcMonitorClientService;
import com.xl.utils.PropertyKit;
import com.xl.utils.SysPropertyConfig;
import common.server.IServerControl;
import org.junit.Before;

/**
 * Created by Caedmon on 2015/8/26.
 */
public class BasicTest {
    protected static final InternalContainer container=InternalContainer.getInstance();
    protected static IServerControl syncServerControl;
    protected static IServerControl asyncServerControl;
    protected static IRpcMonitorClientService IRpcMonitorClientService;
    @Before
    public void init(){
        SysPropertyConfig.doConfig(PropertyKit.getProperties("rpc.properties"));
        InternalContainer container=InternalContainer.getInstance();
        container.startRpcServer(SysPropertyConfig.getProperties());
        container.startRpcClient(SysPropertyConfig.getProperties());
        syncServerControl =container.getSyncRemoteCallProxy(IServerControl.class);
        asyncServerControl=container.getAsyncRemoteCallProxy(IServerControl.class);
        IRpcMonitorClientService =container.getSyncRemoteCallProxy(IRpcMonitorClientService.class);
    }

}
