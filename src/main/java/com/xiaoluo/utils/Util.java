package com.xiaoluo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zwc on 2015/7/13.
 */
public class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);
    static SimpleDateFormat sf = new SimpleDateFormat("yyyy:mm:dd hh:mm:ss:SSS ");

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String loadFromDisk(String path) throws IOException {
        byte[] encoded = null;
        encoded = Files.readAllBytes(Paths.get(path));
        if (encoded != null)
            return new String(encoded);
        return null;
    }

    public static void saveToDisk(String data, String path) throws IOException {
        PrintWriter out = new PrintWriter(path);
        out.print(data);
        out.flush();
        out.close();
    }

    public static boolean ifNotExistCreate(String path) throws IOException {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            return file.createNewFile();
        }
        return true;
    }

    public static void checkDuplicate(String path) {
        try {
            // 在ClassPath搜文件
            Enumeration<URL>
                    urls = Thread.currentThread().getContextClassLoader().getResources(path);
            Set files = new HashSet();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String file = url.getFile();
                    if (file != null && file.length() != 0) {
                        files.add(file);
                    }
                }
            }
            // 如果有多个，就表示重复
            if (files.size() > 1) {
                logger.error("Duplicate class {} in {} jar {}",path,files.size(),files);
            }
        } catch (Throwable e) { // 防御性容错
            logger.error(e.getMessage(), e);
        }
    }
}
