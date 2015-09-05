package log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.xl.rpc.internal.InternalContainer;
import common.server.IServerControl;
import common.server.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

/**
 * Created by Administrator on 2015/8/26.
 */
public class SocketLogClient {
    private static final Logger log= LoggerFactory.getLogger(SocketLogClient.class);
    public static void main(String[] args) {
//        InternalContainer container=InternalContainer.getInstance();
//        container.startRpcServer("rpc.properties");
//        container.startRpcClient("rpc.properties");
//        IServerControl serverControl=container.getSyncRemoteCallProxy(IServerControl.class);
//
//        log.info("Test1 Test1");
//
//        serverControl.testEnum(Name.Caedmon);
    }
}
