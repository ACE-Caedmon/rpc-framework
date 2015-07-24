package cluster;

import com.xl.annotation.MsgType;
import com.xl.cluster.client.SimpleRpcClientApi;
import com.xl.cluster.server.SimpleRpcServerApi;
import common.UserInfo;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

/**
 * Created by Administrator on 2015/7/17.
 */
public class ClusterServer {
    public static void main(String[] args) {
        PropertyConfigurator.configure("conf/log4j.properties");
        SimpleRpcServerApi api=new SimpleRpcServerApi("conf/rpc-server.properties");
        api.bind();
    }
}
