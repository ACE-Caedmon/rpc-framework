package com.xl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Caedmon on 2016/1/13.
 */
public class NetworkUtil {
    private static final Logger log= LoggerFactory.getLogger(NetworkUtil.class);
    public static String getLocalHost() throws SocketException{
        for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback()|| !networkInterface.isUp()) {
                continue;
            }
            Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();
            while (enumeration.hasMoreElements()) {
                InetAddress inetAddress=enumeration.nextElement();
                if(inetAddress instanceof Inet4Address){
                    String hostAddress=inetAddress.getHostAddress();
                    if(!hostAddress.contains("localhost")&&!hostAddress.contains("127.0.0.1")){
                        log.debug("Get local host {}",hostAddress);
                        return hostAddress;
                    }
                }


            }
        }
        return null;
    }

    public static void main(String[] args) throws SocketException{
        System.out.println(getLocalHost());
    }
}
