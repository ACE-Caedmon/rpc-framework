package log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SimpleSocketServer;
import com.xl.rpc.log.RpcLogConverter;

import java.net.URL;

/**
 * Created by Administrator on 2015/8/26.
 */
public class SocketLogServer {
    public static void main(String[] args) throws Exception{
//        XlServerSocketReceiver receiver=new XlServerSocketReceiver();
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        lc.reset();
//        JoranConfigurator configurator = new JoranConfigurator();
//        configurator.setContext(lc);
//        URL url=Thread.currentThread().getContextClassLoader().getResource("logback.xml");
//        configurator.doConfigure(url.getFile());
//        receiver.setContext(lc);
//        receiver.start();
        PatternLayout.defaultConverterMap.put("serviceName",RpcLogConverter.ServiceNameConvert.class.getName());
        PatternLayout.defaultConverterMap.put("address", RpcLogConverter.AddressConvert.class.getName());
        URL url=Thread.currentThread().getContextClassLoader().getResource("logback.xml");
        SimpleSocketServer.main(new String[]{"8082", url.getFile()});
    }
}
