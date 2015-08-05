package spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Administrator on 2015/7/31.
 */
public class SpringRmiServer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context=new ClassPathXmlApplicationContext("server-beans.xml");
    }
}
