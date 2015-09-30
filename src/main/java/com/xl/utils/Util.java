package com.xl.utils;

import com.xl.rpc.exception.BaseException;
import com.xl.rpc.internal.InternalContainer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zwc on 2015/7/13.
 */
public class Util {
    static SimpleDateFormat sf = new SimpleDateFormat("yyyy:mm:dd hh:mm:ss:SSS ");
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String loadFromDisk(String path) throws IOException{
        byte[] encoded = null;
            encoded = Files.readAllBytes(Paths.get(path));
        if (encoded != null)
            return new String(encoded);
        return null;
    }
    public static void saveToDisk(String data,String path) throws IOException{
            PrintWriter out = new PrintWriter(path);
            out.print(data);
            out.flush();
            out.close();
    }
    public static boolean ifNotExistCreate(String path) throws IOException{
        File file=new File(path);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if(!file.exists()){
            return file.createNewFile();
        }
        return true;
    }
}
