package cluster;

import com.xl.rpc.cluster.client.SimpleRpcClientApi;
import com.xl.rpc.internal.InternalContainer;
import common.server.ISameControl;
import common.server.IServerControl;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Administrator on 2015/7/17.
 */
public class ClusterClient {
    private static final Logger log= LoggerFactory.getLogger(ClusterClient.class);
    public static void main(String[] args) throws Exception{
        final InternalContainer container=InternalContainer.getInstance();
        container.startRpcClient("rpc.properties");
        final int loop=1;
        final TimeUse timeUse=new TimeUse();
        int totalUse=0;
        for(int i=0;i<loop;i++){
            final int count=i;
            long start=System.currentTimeMillis();
            String input="test"+count;
            IServerControl serverControl=container.getSyncRemoteCallProxy(IServerControl.class);
            Object result=serverControl.testLong(1L);
            System.out.println(result);
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
