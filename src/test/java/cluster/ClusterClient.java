package cluster;

import com.xl.cluster.client.SimpleRpcClientApi;
import com.xl.message.LoginProtoBuffer;
import common.client.Command;
import common.server.IServerControl;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2015/7/17.
 */
public class ClusterClient {
    private static final Logger log= LoggerFactory.getLogger(ClusterClient.class);
    public static void main(String[] args) throws Exception{
        PropertyConfigurator.configure("log4j.properties");
        final SimpleRpcClientApi api=SimpleRpcClientApi.getInstance().load("rpc-client.properties");
        api.bind();
        final int loop=100;
        final TimeUse timeUse=new TimeUse();
        int totalUse=0;
        for(int i=0;i<loop;i++){
            final int count=i;
            long start=System.currentTimeMillis();
            String input="test"+count;
            IServerControl serverControl=api.getRemoteCallProxy(IServerControl.class);
            Map map=serverControl.login("json","json");
            long end=System.currentTimeMillis();
            int use=(int)(end-start);

            if(use>timeUse.max){
                timeUse.max=use;
            }
            if(use<timeUse.min){
                timeUse.min=use;
            }
            totalUse=totalUse+use;
            log.debug("调用耗时:"+use);
        }
        System.out.println("调用次数:"+loop+",最大耗时:"+timeUse.max+",最小耗时:"+timeUse.min+",总耗时:"+totalUse+",平均耗时:"+totalUse/loop);
    }
    public static class TimeUse{
        public int max;
        public int min=100000;
    }
}
