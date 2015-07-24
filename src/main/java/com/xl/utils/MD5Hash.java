package com.xl.utils;

import redis.clients.util.SafeEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Administrator on 2015/7/22.
 */
public class MD5Hash {
    ThreadLocal<MessageDigest> md5Holder = new ThreadLocal();
    public long hash(String key) {
        return this.hash(SafeEncoder.encode(key));
    }

    public long hash(byte[] key) {
        try {
            if(md5Holder.get() == null) {
                md5Holder.set(MessageDigest.getInstance("MD5"));
            }
        } catch (NoSuchAlgorithmException var6) {
            throw new IllegalStateException("++++ no md5 algorythm found");
        }

        MessageDigest md5 = md5Holder.get();
        md5.reset();
        md5.update(key);
        byte[] bKey = md5.digest();
        long res = (long)(bKey[3] & 255) << 24 | (long)(bKey[2] & 255) << 16 | (long)(bKey[1] & 255) << 8 | (long)(bKey[0] & 255);
        return res;
    }
}
