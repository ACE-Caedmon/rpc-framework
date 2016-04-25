package scan;

import com.xiaoluo.rpc.annotation.RpcService;
import com.xiaoluo.rpc.annotation.RpcMethod;
import com.xiaoluo.utils.ClassUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Administrator on 2015/7/10.
 */
public class AnnotationTest {
    @Test
    public void scanAnnotationClasses() throws Exception{
//        String basePackage="scan";
//        List<Class> result= ClassUtils.findClassesByAnnotation(RpcService.class,basePackage);
//        for(Class c:result){
//            System.out.println(c.getName());
//            Assert.assertEquals(true, ClassUtils.hasAnnotation(c, RpcService.class));
//        }
    }
    @Test
    public void scanAnnotationMethod() throws Exception{
        String basePackage="scan";
        List<Class> classes= ClassUtils.findClassesByAnnotation(RpcService.class, basePackage);
        for(Class c:classes){
            System.out.println(c.getName());
            Assert.assertEquals(true, ClassUtils.hasAnnotation(c, RpcService.class));
            List<Method> methods=ClassUtils.findMethodsByAnnotation(c,RpcMethod.class);
            for(Method m:methods){
                System.out.println(m.getName());
                Assert.assertEquals(true, ClassUtils.hasAnnotation(m, RpcMethod.class));
            }
            System.out.println("----------------");
        }
    }
}
