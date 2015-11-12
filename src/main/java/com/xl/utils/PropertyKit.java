package com.xl.utils;

import com.alibaba.fastjson.JSONObject;
import com.xl.rpc.internal.InternalContainer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Administrator on 2015/5/12.
 */
public class PropertyKit {
    private static Map<String,Properties> cache=new HashMap<>();
    public static String getProperty(String path,String key){
        Properties properties=getProperties(path);
        return properties.getProperty(key);
    }
    public static   Properties getSuitableProperties(String profile,String...configs){
        Properties suitableProperties=null;
        Properties[] propertiesArray=loadProperties(configs);
        for(Properties properties:propertiesArray){
            if(properties.containsKey("server.profile")){
                String value=properties.getProperty("server.profile");
                if(value.trim().toLowerCase().equals(profile)){
                    suitableProperties=properties;
                    break;
                }
            }
        }
        if(suitableProperties==null){
            throw new IllegalArgumentException("No suitable profile found '"+profile+"'");
        }
        return suitableProperties;
    }
    public static Properties parseProperties(String content) throws IOException {
        InputStream inputStream=new ByteArrayInputStream(content.getBytes());
        Properties properties=new Properties();
        properties.load(inputStream);
        inputStream.close();
        return properties;
    }
    public static Properties[] loadProperties(String...configs){
        Properties[] propertiesArray=new Properties[configs.length];
        for(int i=0;i<propertiesArray.length;i++){
            Properties properties= PropertyKit.getProperties(configs[i]);
            propertiesArray[i]=properties;
        }
        return propertiesArray;
    }
    public static void removeProperty(String path,String key){
        Properties properties= getProperties(path);
        properties.remove(key);
        OutputStream out=null;
        try {
            out = new FileOutputStream(new File(path));
            properties.store(out, "Remove property ( key = "+key+")");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            try {
                out.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static void setProperty(String path,String key,String value){
        Properties properties= getProperties(path);
        properties.setProperty(key, value);
        OutputStream out=null;
        try {
            out = new FileOutputStream(new File(path));
            properties.store(out, "Update property ( key = "+key+", value = "+value+")");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            try {
                out.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static Properties getProperties(String path){
        Properties properties=cache.get(path);
        if(properties==null){

            properties=loadProperties(path);
            cache.put(path,properties);
        }

        return properties;
    }
    public static Properties loadProperties(String path){
        Properties properties=new Properties();
        InputStream in=PropertyKit.class.getClassLoader().getResourceAsStream(path);
        return loadProperties(in);
    }
    public static Properties loadProperties(InputStream inputStream) {
        Properties properties=new Properties();
        try{
            properties.load(inputStream);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return properties;
    }
    public static JSONObject properties2Json(String path){
        Properties properties= getProperties(path);
        JSONObject result=new JSONObject();
        for(Map.Entry<Object, Object> entry:properties.entrySet()){
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
}
