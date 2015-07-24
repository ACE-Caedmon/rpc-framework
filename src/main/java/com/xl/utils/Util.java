package com.xl.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

    public static String getFileData(String path) {
        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(Paths.get(path));
            if (encoded != null)
                return new String(encoded);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void saveFileData(String path,String data) {
        try {
            PrintWriter out = new PrintWriter(path);
            out.print(data);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
