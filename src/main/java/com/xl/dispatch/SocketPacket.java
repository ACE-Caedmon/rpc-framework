package com.xl.dispatch;

import com.xl.annotation.MsgType;
import com.xl.utils.NGSocketParams;

/**
 * Created by Administrator on 2015/7/16.
 */
public class SocketPacket {
    protected int cmd;
    protected MsgType msgType= NGSocketParams.getSystemMsgType();
    protected Object[] params;
    public SocketPacket(int cmd,Object...params){
        this.cmd=cmd;
        this.params=params;
    }
    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
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
