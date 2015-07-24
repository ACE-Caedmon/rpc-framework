package com.xl.annotation;

/**
 * Created by Caedmon on 2015/7/11.
 */
public enum MsgType {
    JSON(1),ProtoBuf(2),Binary(3);
    public int value;
    MsgType(int value){
        this.value=value;
    }
    public static MsgType valueOf(int value){
        switch (value){
            case 1:
                return JSON;
            case 2:
                return ProtoBuf;
            case 3:
                return Binary;
            default:
                return JSON;
        }
    }
}
