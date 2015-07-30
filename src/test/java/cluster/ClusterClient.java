package cluster;

import com.xl.cluster.client.SimpleRpcClientApi;
import common.server.IServerControl;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final int loop=1;
        final TimeUse timeUse=new TimeUse();
        final AtomicInteger totalUse=new AtomicInteger();
        for(int i=0;i<loop;i++){
            final int count=i;
            Thread t=new Thread(new Runnable() {
                @Override
                public void run() {
                    long start=System.currentTimeMillis();
                    String input="test"+count;
                    IServerControl serverControl=api.getRemoteCallProxy(IServerControl.class);
                    serverControl.login("username", "password");
                    try{
                        Thread.sleep(10000);
                    }catch (Exception e){

                    }

                    serverControl.login("username2","password2");
                    long end=System.currentTimeMillis();
                    int use=(int)(end-start);
                    if(use>timeUse.max&&use<3000){
                        timeUse.max=use;
                    }
                    if(use<timeUse.min){
                        timeUse.min=use;
                    }
                    totalUse.getAndAdd(use);
                }
            });
            t.start();
        }
        Thread.sleep(10000);
        System.out.println("并发量:"+loop+",最大耗时:"+timeUse.max+",最小耗时:"+timeUse.min+",平均耗时:"+totalUse.get()/loop);
    }
    public static class TimeUse{
        public int max;
        public int min=100000;
    }
}
