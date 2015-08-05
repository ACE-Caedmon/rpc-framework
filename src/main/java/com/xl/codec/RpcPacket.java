package com.xl.codec;

import com.alibaba.fastjson.JSON;
import com.xl.dispatch.SocketPacket;

import java.util.Arrays;

/**
 * @author Caedmon
 * 输出对象
 * */
public class RpcPacket extends SocketPacket {
    private boolean fromCall;
    private boolean sync;
    private String uuid;
    private boolean exception;
    private String[] classNameArray;
    public RpcPacket(String cmd,Object...content){
        super(cmd,content);
    }

    public boolean getSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isException() {
        return exception;
    }

    public void setException(boolean exception) {
        this.exception = exception;
    }

    public String[] getClassNameArray() {
        return classNameArray;
    }

    public void setClassNameArray(String[] classNameArray) {
        this.classNameArray = classNameArray;
    }

    public boolean isFromCall() {
        return fromCall;
    }

    public void setFromCall(boolean fromCall) {
        this.fromCall = fromCall;
    }

    public String getClassNames(){
        return Arrays.toString(classNameArray);
    }

    @Override
    public String toString() {
        return "RpcPacket{" +
                "classNameArray=" + Arrays.toString(classNameArray) +
                ", exception=" + exception +
                ", uuid='" + uuid + '\'' +
                ", sync=" + sync +
                ", fromCall=" + fromCall +
                '}';
    }
}

