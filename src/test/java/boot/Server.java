package boot;

import com.xl.boot.ServerSettings;
import com.xl.boot.ServerSocketEngine;
import com.xl.boot.SocketEngine;
import com.xl.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.dispatch.method.SpringBeanAccess;
import interceptor.LoginInterceptor;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Caedmon on 2015/7/12.
 */
public class Server {
    public static void main(String[] args) {
        PropertyConfigurator.configure("conf/log4j.properties");
        ServerSettings serverSettings=new ServerSettings();
        serverSettings.protocol= SocketEngine.TCP_PROTOCOL;
        serverSettings.port=8001;
        serverSettings.scanPackage=new String[]{"common.server"};
        System.setProperty("ng.socket.netty.loggging", "false");
        ServerSocketEngine engine=new ServerSocketEngine(serverSettings,new JavassitRpcMethodDispatcher(new SpringBeanAccess(),10));
        engine.addCmdMethodInterceptor(new LoginInterceptor());
        engine.start();
    }
}
