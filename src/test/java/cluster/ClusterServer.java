package cluster;

import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import com.xl.rpc.internal.InternalContainer;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Administrator on 2015/7/17.
 */
public class ClusterServer {
    public static void main(String[] args) {
        System.setProperty("javassit.writeClass", "true");
        InternalContainer container=InternalContainer.getInstance();
        container.startRpcServer("rpc.properties");
    }
}
