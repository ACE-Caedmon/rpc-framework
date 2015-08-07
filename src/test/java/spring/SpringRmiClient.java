package spring;

import common.server.IServerControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Administrator on 2015/7/31.
 */
public class SpringRmiClient {
    private static final Logger log= LoggerFactory.getLogger(SpringRmiClient.class);
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context=new ClassPathXmlApplicationContext("client-beans.xml");

        IServerControl serverControl=(IServerControl)context.getBean("remoteServerControl");
        long start=System.currentTimeMillis();
        final int loop=100;
        int totalUse=0;
        TimeUse timeUse=new TimeUse();
        for(int i=0;i<loop;i++){
            serverControl.login("Caedmon","123456");
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
        long end=System.currentTimeMillis();
        System.out.println("调用次数:"+loop+",最大耗时:"+timeUse.max+",最小耗时:"+timeUse.min+",总耗时:"+totalUse+",平均耗时:"+totalUse/loop);
    }
    public static class TimeUse{
        public int max;
        public int min=100000;
    }
}
