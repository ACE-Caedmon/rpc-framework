package scan;

import com.xl.annotation.CmdControl;
import com.xl.annotation.CmdMethod;
import com.xl.utils.ClassUtils;
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
//        List<Class> result= ClassUtils.findClassesByAnnotation(CmdControl.class,basePackage);
//        for(Class c:result){
//            System.out.println(c.getName());
//            Assert.assertEquals(true, ClassUtils.hasAnnotation(c, CmdControl.class));
//        }
    }
    @Test
    public void scanAnnotationMethod() throws Exception{
//        String basePackage="scan";
//        List<Class> classes= ClassUtils.findClassesByAnnotation(CmdControl.class,basePackage);
//        for(Class c:classes){
//            System.out.println(c.getName());
//            Assert.assertEquals(true, ClassUtils.hasAnnotation(c, CmdControl.class));
//            List<Method> methods=ClassUtils.findMethodsByAnnotation(c,CmdMethod.class);
//            for(Method m:methods){
//                System.out.println(m.getName());
//                Assert.assertEquals(true, ClassUtils.hasAnnotation(m, CmdMethod.class));
//            }
//            System.out.println("----------------");
//        }
    }
}
