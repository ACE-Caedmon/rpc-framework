package log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggerContextAwareBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.xl.rpc.internal.InternalContainer;
import common.server.IServerControl;
import common.server.Name;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;

/**
 * Created by Administrator on 2015/8/26.
 */
public class SocketLogClient {
    private static final Logger log= LoggerFactory.getLogger(SocketLogClient.class);
    public static void main(String[] args) {
        InternalContainer container=InternalContainer.getInstance();
        container.startRpcServer("rpc.properties");
        container.startRpcClient("rpc.properties");
        IServerControl serverControl=container.getSyncRemoteCallProxy(IServerControl.class);
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            context.reset();
            String config=Thread.currentThread().getContextClassLoader().getResource("remote-logback.xml").getFile();
            configurator.doConfigure(config);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }

        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        MDC.put("1", "1MDC");
        log.info("Test1 Test1");

        serverControl.testEnum(Name.Caedmon);
    }
}
