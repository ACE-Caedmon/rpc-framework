package com.xl.rpc.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.*;
import com.xl.rpc.cluster.server.RpcServerApi;
import com.xl.rpc.internal.InternalContainer;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by Administrator on 2015/8/27.
 */
public class RpcLogEventVO implements ILoggingEvent,Serializable{
    private static final long serialVersionUID = 6553722650255690312L;
    private static final int NULL_ARGUMENT_ARRAY = -1;
    private static final String NULL_ARGUMENT_ARRAY_ELEMENT = "NULL_ARGUMENT_ARRAY_ELEMENT";
    private String threadName;
    private String loggerName;
    private LoggerContextVO loggerContextVO;
    private transient Level level;
    private String message;
    private transient String formattedMessage;
    private transient Object[] argumentArray;
    private ThrowableProxyVO throwableProxy;
    private StackTraceElement[] callerDataArray;
    private Marker marker;
    private Map<String, String> mdcPropertyMap;
    private long timeStamp;
    private String serviceName;
    private String address;
    public RpcLogEventVO() {
    }

    public static RpcLogEventVO build(ILoggingEvent le) {
        RpcLogEventVO ledo = new RpcLogEventVO();
        ledo.loggerName = le.getLoggerName();
        ledo.loggerContextVO = le.getLoggerContextVO();
        ledo.threadName = le.getThreadName();
        ledo.level = le.getLevel();
        ledo.message = le.getMessage();
        ledo.argumentArray = le.getArgumentArray();
        ledo.marker = le.getMarker();
        ledo.mdcPropertyMap = le.getMDCPropertyMap();
        ledo.timeStamp = le.getTimeStamp();
        ledo.throwableProxy = ThrowableProxyVO.build(le.getThrowableProxy());
        RpcServerApi rpcServerApi=InternalContainer.getInstance().getRpcServerApi();
        if(rpcServerApi!=null&&rpcServerApi.isStarted()){
            ledo.serviceName= Arrays.toString(rpcServerApi.getClusterNames().toArray());
        }else{
            String systemServiceName=System.getProperty("rpc.log.serviceName");
            ledo.serviceName=systemServiceName==null?"Not specify":systemServiceName;
        }
        if(rpcServerApi!=null&&rpcServerApi.isStarted()){
            ledo.address=rpcServerApi.getHost()+":"+rpcServerApi.getPort();
        }else{
            String systemAddress=System.getProperty("rpc.log.address");
            ledo.address=systemAddress==null?"Not specify":systemAddress;
        }
        if(le.hasCallerData()) {
            ledo.callerDataArray = le.getCallerData();
        }

        return ledo;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public void setLoggerContextVO(LoggerContextVO loggerContextVO) {
        this.loggerContextVO = loggerContextVO;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setArgumentArray(Object[] argumentArray) {
        this.argumentArray = argumentArray;
    }

    public void setThrowableProxy(ThrowableProxyVO throwableProxy) {
        this.throwableProxy = throwableProxy;
    }

    public void setCallerDataArray(StackTraceElement[] callerDataArray) {
        this.callerDataArray = callerDataArray;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void setMdcPropertyMap(Map<String, String> mdcPropertyMap) {
        this.mdcPropertyMap = mdcPropertyMap;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public LoggerContextVO getLoggerContextVO() {
        return this.loggerContextVO;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    public Level getLevel() {
        return this.level;
    }

    public String getMessage() {
        return this.message;
    }

    public String getFormattedMessage() {
        if(this.formattedMessage != null) {
            return this.formattedMessage;
        } else {
            if(this.argumentArray != null) {
                this.formattedMessage = MessageFormatter.arrayFormat(this.message, this.argumentArray).getMessage();
            } else {
                this.formattedMessage = this.message;
            }

            return this.formattedMessage;
        }
    }

    public Object[] getArgumentArray() {
        return this.argumentArray;
    }

    public IThrowableProxy getThrowableProxy() {
        return this.throwableProxy;
    }

    public StackTraceElement[] getCallerData() {
        return this.callerDataArray;
    }

    public boolean hasCallerData() {
        return this.callerDataArray != null;
    }

    public Marker getMarker() {
        return this.marker;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public long getContextBirthTime() {
        return this.loggerContextVO.getBirthTime();
    }

    public LoggerContextVO getContextLoggerRemoteView() {
        return this.loggerContextVO;
    }

    public Map<String, String> getMDCPropertyMap() {
        return this.mdcPropertyMap;
    }

    public Map<String, String> getMdc() {
        return this.mdcPropertyMap;
    }

    public void prepareForDeferredProcessing() {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(this.level.levelInt);
        if(this.argumentArray != null) {
            int len = this.argumentArray.length;
            out.writeInt(len);

            for(int i = 0; i < this.argumentArray.length; ++i) {
                if(this.argumentArray[i] != null) {
                    out.writeObject(this.argumentArray[i].toString());
                } else {
                    out.writeObject("NULL_ARGUMENT_ARRAY_ELEMENT");
                }
            }
        } else {
            out.writeInt(-1);
        }

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int levelInt = in.readInt();
        this.level = Level.toLevel(levelInt);
        int argArrayLen = in.readInt();
        if(argArrayLen != -1) {
            this.argumentArray = new String[argArrayLen];

            for(int i = 0; i < argArrayLen; ++i) {
                Object val = in.readObject();
                if(!"NULL_ARGUMENT_ARRAY_ELEMENT".equals(val)) {
                    this.argumentArray[i] = val;
                }
            }
        }

    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        int result1 = 31 * result + (this.message == null?0:this.message.hashCode());
        result1 = 31 * result1 + (this.threadName == null?0:this.threadName.hashCode());
        result1 = 31 * result1 + (int)(this.timeStamp ^ this.timeStamp >>> 32);
        return result1;
    }

    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(obj == null) {
            return false;
        } else if(this.getClass() != obj.getClass()) {
            return false;
        } else {
            RpcLogEventVO other = (RpcLogEventVO)obj;
            if(this.message == null) {
                if(other.message != null) {
                    return false;
                }
            } else if(!this.message.equals(other.message)) {
                return false;
            }

            if(this.loggerName == null) {
                if(other.loggerName != null) {
                    return false;
                }
            } else if(!this.loggerName.equals(other.loggerName)) {
                return false;
            }

            if(this.threadName == null) {
                if(other.threadName != null) {
                    return false;
                }
            } else if(!this.threadName.equals(other.threadName)) {
                return false;
            }

            if(this.timeStamp != other.timeStamp) {
                return false;
            } else {
                if(this.marker == null) {
                    if(other.marker != null) {
                        return false;
                    }
                } else if(!this.marker.equals(other.marker)) {
                    return false;
                }

                if(this.mdcPropertyMap == null) {
                    if(other.mdcPropertyMap != null) {
                        return false;
                    }
                } else if(!this.mdcPropertyMap.equals(other.mdcPropertyMap)) {
                    return false;
                }

                return true;
            }
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
