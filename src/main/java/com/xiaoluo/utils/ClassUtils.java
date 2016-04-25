package com.xiaoluo.utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by Caedmon on 2015/7/10.
 */
public class ClassUtils {
    private static final Logger log= LoggerFactory.getLogger(ClassUtils.class);
    public static final Map<Class,Class> PRIMITIVE_CLASS_CACHE =new HashMap<>();
    public static final Map<Class,Class> PACKING_CLASS_CACHE=new HashMap<>();
    public static final Set<String> INVAILD_PACKAGE_NAMES=new HashSet<>();
    public static ClassLoader scanClassLoader =ClassUtils.class.getClassLoader();
    public static String scanClassPath =null;
    static {
        PRIMITIVE_CLASS_CACHE.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_CLASS_CACHE.put(Byte.TYPE, Byte.class);
        PRIMITIVE_CLASS_CACHE.put(Character.TYPE, Character.class);
        PRIMITIVE_CLASS_CACHE.put(Double.TYPE, Double.class);
        PRIMITIVE_CLASS_CACHE.put(Float.TYPE, Float.class);
        PRIMITIVE_CLASS_CACHE.put(Integer.TYPE, Integer.class);
        PRIMITIVE_CLASS_CACHE.put(Long.TYPE, Long.class);
        PRIMITIVE_CLASS_CACHE.put(Short.TYPE, Short.class);
        INVAILD_PACKAGE_NAMES.add("java.lang.");
        PACKING_CLASS_CACHE.put(Boolean.class, Boolean.TYPE);
        PACKING_CLASS_CACHE.put(Byte.class,Byte.TYPE);
        PACKING_CLASS_CACHE.put(Character.class,Character.TYPE);
        PACKING_CLASS_CACHE.put(Double.class,Double.TYPE);
        PACKING_CLASS_CACHE.put(Float.class,Float.TYPE);
        PACKING_CLASS_CACHE.put(Integer.class,Integer.TYPE);
        PACKING_CLASS_CACHE.put(Long.class,Long.TYPE);
        PACKING_CLASS_CACHE.put(Short.class, Short.TYPE);

    }
    public static List<Class> getClasssFromPackage(String...packageNames) throws Exception{
        List<Class> classes = new ArrayList<>();
        List<File> allFiles=new ArrayList<>();
        String osName=System.getProperty("os.name");
        String split=":";
        if(osName.toLowerCase().startsWith("windows")){
            split=";";
        }
        if(scanClassPath==null){
            scanClassPath=System.getProperty("java.class.path");
        }
        String[] classPaths=scanClassPath.split(split);
        for(String classpath:classPaths){
            File cpf=new File(classpath);
            listDirectory(cpf, allFiles);
        }
        for(File f:allFiles){
            String name=f.getName();
            if(name.endsWith(".jar")){
                findClasssFromJarFile(f, classes, packageNames);
            }
            if(name.endsWith(".class")){
                boolean find=false;
                for(String classPath:classPaths){
                    //表示在classpath中
                    if(f.getAbsolutePath().contains(classPath)){
                        String className = f.getAbsolutePath().substring(classPath.length()+1);
                        className = className.replace(File.separatorChar, '.').replaceAll(".class","");
                        try{
                            Class c= scanClassLoader.loadClass(className);
                            for(String pkg:packageNames){
                                Package classPackage=c.getPackage();
                                if(classPackage!=null){
                                    if(classPackage.getName().startsWith(pkg)){
                                        classes.add(c);
                                    }
                                }else{
                                    if(pkg.trim().equals("")){
                                        classes.add(c);
                                    }
                                }

                            }
                        }catch (ClassNotFoundException e){

                        }finally {
                            find=true;
                        }
                        break;
                    }
                }
                if(!find){
                    String className = f.getAbsolutePath().substring(0, f.getName().length() - 6);
                    log.info(className);
                    Class c= scanClassLoader.loadClass(className);
                    for(String pkg:packageNames){
                        if(c.getPackage().getName().startsWith(pkg)){
                            classes.add(c);
                        }
                    }
                }

            }
        }

        return classes;
    }
    public static File[] getClassPathJars() throws Exception{
        URL classPath=ClassUtils.class.getResource("/");
        String jarFilePath = ClassUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
        File libDir=new File(new File(jarFilePath).getParent());
        return libDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
    }
    public static void listDirectory(File path, List<File> myfile){
        if (!path.exists()){
            System.out.println("文件名称不存在!");
        }
        else
        {
            if (path.isFile()){
                myfile.add(path);
            } else{
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++  ){
                    listDirectory(files[i], myfile);
                }
            }
        }
    }
    public static void main(String[] args) throws Exception{
        List<Class> classes=new ArrayList<>();
        findClasssFromJarFile(new File("lib/common-1.0-SNAPSHOT.jar"),classes,"com.xiaoluo");
    }
    public static List<Class> findClasssFromJarFile(File jarPath,List<Class> classes,String... packageNames) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        List<JarEntry> jarEntryList = new ArrayList<JarEntry>();

        Enumeration<JarEntry> ee = jarFile.entries();
        while (ee.hasMoreElements()) {
            JarEntry entry = ee.nextElement();
            // 过滤我们出满足我们需求的东西
            for(String packageName:packageNames){
                packageName=packageName.replace('.','/');
                if (entry.getName().startsWith(packageName) && entry.getName().endsWith(".class")) {
                    jarEntryList.add(entry);
                }
            }
        }

        for (JarEntry entry : jarEntryList) {
            String className = entry.getName().replace('/', '.');
            className = className.substring(0, className.length() - 6);
            try {
                if(className.equals("org.eclipsswt.dnd.ByteArrayTransfer")){
                    System.out.println("!!!!!!!!!!!!!!!!!!!!");
                }
                classes.add(scanClassLoader.loadClass(className));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return classes;
    }
    public static List<Class> findClassesByAnnotation(Class<? extends Annotation> ac,String... packageName) throws Exception{
        List<Class> allClasses=getClasssFromPackage(packageName);
        List<Class> result=new ArrayList<>();
        for(Class c:allClasses){
            if(hasAnnotation(c,ac)){
                result.add(c);
            }
        }
        return result;
    }
    /**
     * 交集
     * */
    public static List<Class> findIntersectionClassesByAnnotation(Class<? extends Annotation>[] ac,String... packageName) throws Exception{
        List<Class> allClasses=getClasssFromPackage(packageName);
        List<Class> result=new ArrayList<>();
        for(Class c:allClasses){
            for(Class<? extends Annotation> a:ac){
                if(hasAnnotation(c,a)){
                    result.add(c);
                }
            }
        }
        return result;
    }
    public static List<Class> findUnionClassesByAnnotation(Class<? extends Annotation>[] ac,String... packageName) throws Exception{
        List<Class> allClasses=getClasssFromPackage(packageName);
        List<Class> result=new ArrayList<>();
        for(Class c:allClasses){
            for(Class<? extends Annotation> a:ac){
                if(!hasAnnotation(c,a)){
                    break;
                }
                result.add(c);
            }
        }
        return result;
    }
    /**
     * 在package对应的路径下找到所有的class
     *
     * @param packageName
     *            package名称
     * @param filePath
     *            package对应的路径
     * @param recursive
     *            是否查找子package
     * @param clazzs
     *            找到class以后存放的集合
     */
    public static void findClassInPackageByFile(String packageName, String filePath, final boolean recursive, List<Class> clazzs) {
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 在给定的目录下找到所有的文件，并且进行条件过滤
        File[] dirFiles = dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean acceptDir = recursive && file.isDirectory();// 接受dir目录
                boolean acceptClass = file.getName().endsWith("class");// 接受class文件
                return acceptDir || acceptClass;
            }
        });

        for (File file : dirFiles) {
            if (file.isDirectory()) {
                findClassInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, clazzs);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    clazzs.add(scanClassLoader.loadClass(packageName + "." + className));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static List<Method> findMethodsByAnnotation(Class c,Class annotationClass){
        Method[] methods=c.getDeclaredMethods();
        List<Method> result=new ArrayList<>();
        for(Method m:methods){
            if(hasAnnotation(m,annotationClass)){
                result.add(m);
            }
        }
        return result;
    }
    public static <T extends Annotation> T getAnnotation(Class c,Class<T> annotationClass){
        T result=(T)c.getAnnotation(annotationClass);
        return result;
    }
    public static boolean hasAnnotation(Class c,Class annotationClass){
        return getAnnotation(c,annotationClass)!=null;
    }
    public static Annotation getAnnotation(Method m,Class<? extends Annotation> annotationClass){
        Annotation result=m.getAnnotation(annotationClass);
        return result;
    }
    public static boolean hasAnnotation(Method m,Class annotationClass){
        return getAnnotation(m,annotationClass)!=null;
    }
    public static <T extends Annotation> T getAnnotation(Field f,Class<T> annotationClass){
        T result=f.getAnnotation(annotationClass);
        return result;
    }
    public static boolean hasAnnotation(Field f,Class annotationClass){
        return getAnnotation(f,annotationClass)!=null;
    }
    public static boolean checkClassType(Class autualClass,Class...exceptClasses){
        for(Class exceptClass:exceptClasses){
            if(autualClass.isAssignableFrom(exceptClass)){
                return true;
            }
        }
        return false;
    }
    public static boolean isVoidReturn(Class returnType){
        return returnType==Void.TYPE||returnType==Void.class;
    }
    public static boolean hasDeclaredMethod(Class c,String name,Class... paramTypes){
        Method[] methods=c.getDeclaredMethods();
        boolean b=true;
        for(Method m:methods){
            b=b&&m.getName().equals(name);
            if(!b){
                continue;
            }
            b=b&&(paramTypes.length==m.getParameterTypes().length);
            if(!b){
                continue;
            }
            Class[] methodParamTypes=m.getParameterTypes();
            for(int i=0;i<methodParamTypes.length;i++){
                b=b&&(methodParamTypes[i]==paramTypes[i]);
                if(!b){
                    return  false;
                }
            }
            if(b){
                return true;
            }
        }
        return false;
    }
    public static String getCompleteClassName(Class clazz){
        String className=clazz.getName();
        Class primitiveClass= PRIMITIVE_CLASS_CACHE.get(clazz);
        String proxyClassNamePrefix=className;
        //是否为基本数据类型 int,long等
        if(primitiveClass!=null){
            proxyClassNamePrefix=primitiveClass.getName();
        }
        for(String packageName:INVAILD_PACKAGE_NAMES){
            if(proxyClassNamePrefix.startsWith(packageName)){
                proxyClassNamePrefix=proxyClassNamePrefix.replaceFirst(packageName,"");
                break;
            }
        }
        return proxyClassNamePrefix;
    }
    public static boolean isPrimitive(Class type){
        return PRIMITIVE_CLASS_CACHE.containsKey(type);
    }
    public static boolean isPrimitivePackingType(Class type){
        return PACKING_CLASS_CACHE.containsKey(type);
    }
    public static Class getPackingType(Class type){
        if(isPrimitive(type)){
            return PRIMITIVE_CLASS_CACHE.get(type);
        }
        return type;
    }
    public static Class getPrimitiveType(Class type){
        if(isPrimitivePackingType(type)){
            return PACKING_CLASS_CACHE.get(type);
        }
        return type;
    }
    public static String appendClassPath(String classpath){
        if(scanClassPath==null){
            scanClassPath=System.getProperty("java.class.path");
        }
        StringBuilder builder=new StringBuilder(scanClassPath);
        String osName = System.getProperty("os.name");
        String split = ":";
        if(osName.toLowerCase().startsWith("windows")) {
            split = ";";
        }
        builder.append(split).append(classpath);
        scanClassPath=builder.toString();
        return scanClassPath;
    }
}
