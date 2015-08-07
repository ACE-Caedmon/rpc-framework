package com.xl.rpc.dispatch;

import com.xl.rpc.annotation.MsgType;
import com.xl.utils.EngineParams;

/**
 * Created by Administrator on 2015/7/16.
 */
public class SocketPacket {
    protected String cmd;
    protected MsgType msgType= EngineParams.getSystemMsgType();
    protected Object[] params;
    public SocketPacket(String cmd,Object...params){
        this.cmd=cmd;
        this.params=params;
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

    public void setParams(Object... params) {
        this.params = params;
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }
}
