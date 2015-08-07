package com.xl.utils;

import com.xl.rpc.exception.CodecException;
import com.xl.rpc.exception.RpcException;

import java.io.*;

/**
 * Created by Administrator on 2014/6/7.
 */
public class CommonUtils {

    public static String firstToUpperCase(String s){
        return s.replaceFirst( s.substring(0, 1), s.substring(0, 1).toUpperCase());
    }
    public static byte[] serialize(Object object){
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos=new ObjectOutputStream(bos);
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CodecException(e);
        }finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static <T> T derialize(byte[] bytes,Class<T> clazz){
        ByteArrayInputStream bis=new ByteArrayInputStream(bytes);
        Object object=null;
        try {
            ObjectInputStream ois=new ObjectInputStream(bis);
            return (T)ois.readObject();
        } catch (Exception e) {
            throw new CodecException(e);
        }finally {
            try {
                bis.close();
            } catch (IOException e) {
                throw new CodecException(e);
            }
        }
    }
    public static long now(){
        return System.nanoTime();
    }
    public static void main(String[] args) {
        RpcException rpcException=new RpcException(new NullPointerException("test for null"));
        derialize(serialize(rpcException),RpcException.class).printStackTrace();
    }
}
