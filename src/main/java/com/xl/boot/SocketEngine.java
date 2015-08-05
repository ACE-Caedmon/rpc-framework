package com.xl.boot;

import com.xl.annotation.RpcControl;
import com.xl.annotation.Extension;
import com.xl.dispatch.CmdInterceptor;
import com.xl.dispatch.method.RpcMethodDispatcher;
import com.xl.utils.ClassUtils;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Chenlong on 2014/5/19.
 * 网络引擎核心启动类
 */
public abstract class SocketEngine {
    protected Set<ModuleExtension> extensions;
    private static Logger log= LoggerFactory.getLogger(SocketEngine.class);
    public static final String TCP_PROTOCOL="tcp",WEBSOCKET_PROTOCOL="websocket";
    protected RpcMethodDispatcher rpcMethodDispatcher;
    protected EngineSettings settings;
    public SocketEngine(EngineSettings settings,RpcMethodDispatcher rpcMethodDispatcher){
        this.settings=settings;
        this.extensions=new HashSet<>();
        this.rpcMethodDispatcher = rpcMethodDispatcher;
    }
    /**
     * 停止网络服务
     * */
    public void shutdown(){
        for(ModuleExtension extension:extensions){
            extension.destory();
        }
        extensions.clear();
    }
    public void start(){
        load();
        startSocket();

    }
    public abstract void startSocket();
    public void load(){
        //生成
        try{
            String[] scanPackage=settings.scanPackage;
            List<Class> allClasses=ClassUtils.getClasssFromPackage(scanPackage);
            for(Class clazz:allClasses){
                if(ClassUtils.hasAnnotation(clazz,Extension.class)){
                    ModuleExtension extension=(ModuleExtension)clazz.newInstance();
                    extensions.add(extension);
                    log.info("Load extension:{}", StringUtil.simpleClassName(extension));
                    extension.init();
                }
                Class[] controlInterfaceList=clazz.getInterfaces();
                if(!clazz.isInterface()){
                    for(Class controlInterface:controlInterfaceList){
                        if(ClassUtils.hasAnnotation(controlInterface,RpcControl.class)){
                            rpcMethodDispatcher.loadClasses(clazz);
                            log.info("Load control "+clazz.getName());
                            break;
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void addCmdMethodInterceptor(CmdInterceptor interceptor){
        rpcMethodDispatcher.addMethodInterceptor(interceptor);
    }
}
