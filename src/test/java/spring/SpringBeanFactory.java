package spring;

import common.client.ClientControl;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Caedmon on 2015/7/12.
 */
public class SpringBeanFactory {
    @Test
    public void getAutoWireBean() throws Exception{
        ClassPathXmlApplicationContext context=new ClassPathXmlApplicationContext("spring/beans.xml");
        ClientControl control=context.getBean(ClientControl.class);
    }
}
