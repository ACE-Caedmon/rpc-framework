package cluster;

import com.xl.rpc.cluster.server.SimpleRpcServerApi;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Administrator on 2015/7/17.
 */
public class ClusterServer {
    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");
        SimpleRpcServerApi api=new SimpleRpcServerApi("rpc.properties");
        api.bind();
    }
}
