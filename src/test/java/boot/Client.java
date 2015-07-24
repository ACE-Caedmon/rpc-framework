package boot;

import com.xl.boot.TCPClientSettings;
import com.xl.boot.TCPClientSocketEngine;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.dispatch.method.JavassitRpcMethodDispatcher;
import com.xl.dispatch.method.PrototypeBeanAccess;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Caedmon on 2015/7/13.
 */
public class Client {
    public static void main(String[] args) throws Exception{
        PropertyConfigurator.configure("conf/log4j.properties");
        TCPClientSettings settings=new TCPClientSettings();
        settings.port=8001;
        settings.host="localhost";
        settings.scanPackage=new String[]{"common.client"};
        settings.workerThreadSize=2;
        System.setProperty("ng.socket.netty.loggging", "false");
        RpcMethodDispatcher dispatcher=new JavassitRpcMethodDispatcher(new PrototypeBeanAccess(),10);
        for(int i=0;i<1;i++){
            final TCPClientSocketEngine engine=new TCPClientSocketEngine(settings,dispatcher);
            engine.start();
        }
    }
}
