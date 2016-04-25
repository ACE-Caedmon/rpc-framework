package com.xiaoluo.rpc.codec;

import com.xiaoluo.rpc.annotation.MsgType;

import java.util.Arrays;

/**
 * @author Caedmon
 * 输出对象
 * */
public class RpcPacket{
    private boolean fromCall;
    private boolean sync;
    private String uuid;
    private boolean exception;
    private String[] classNameArray;
    private String cmd;
    private MsgType msgType= MsgType.JSON;
    private Object[] params;
    public RpcPacket(String cmd,Object...params){
        this.cmd=cmd;
        this.params=params;
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
    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RpcPacket{");
        sb.append("fromCall=").append(fromCall);
        sb.append(", cmd=").append(cmd);
        sb.append(", uuid='").append(uuid).append('\'');
        //sb.append(", params=").append(Arrays.toString(params));
        sb.append('}');
        return sb.toString();
    }
    public static void validate(Class[] paramTypes,Object... params){
        if(params.length!=paramTypes.length){
            throw new IllegalArgumentException("Params length is "+params.length+",but paramTypes length is "+paramTypes.length);
        }
        for(int i=0;i<params.length;i++){
            if(params[i]==null){
                continue;
            }
            Class actualType=params[i].getClass();
            if(!paramTypes[i].isAssignableFrom(actualType)){
                throw new IllegalArgumentException("Param except type "+paramTypes[i]+",actual type is "+actualType);
            }
        }
    }
}

