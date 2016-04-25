package rpc;

import com.xiaoluo.rpc.internal.InternalContainer;
import com.xiaoluo.rpc.registry.client.IRpcRegistryClientService;
import com.xiaoluo.utils.PropertyKit;
import com.xiaoluo.utils.SysPropertyConfig;
import common.server.IServerControl;
import org.junit.Before;

/**
 * Created by Caedmon on 2015/8/26.
 */
public class BasicTest {
    protected static final InternalContainer container=InternalContainer.getInstance();
    protected static IServerControl syncServerControl;
    protected static IServerControl asyncServerControl;
    protected static IRpcRegistryClientService IRpcRegistryClientService;
    @Before
    public void init(){
        SysPropertyConfig.doConfig(PropertyKit.getProperties("rpc.properties"));
        InternalContainer container=InternalContainer.getInstance();
        container.startRpc(SysPropertyConfig.getProperties());
        syncServerControl =container.getSyncRemoteCallProxy(IServerControl.class);
        asyncServerControl=container.getAsyncRemoteCallProxy(IServerControl.class);
        IRpcRegistryClientService =container.getSyncRemoteCallProxy(IRpcRegistryClientService.class);
    }

}
