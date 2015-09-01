package com.xl.rpc.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.CloseUtil;
import ch.qos.logback.core.util.Duration;

import javax.net.SocketFactory;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2015/8/27.
 */
public class RpcAppender extends AppenderBase<ILoggingEvent> implements Runnable, SocketConnector.ExceptionHandler {
    private  boolean includeCallerData;
    static {
        PatternLayout.defaultConverterMap.put("serviceName",RpcLogConverter.ServiceNameConvert.class.getName());
        PatternLayout.defaultConverterMap.put("address", RpcLogConverter.AddressConvert.class.getName());
    }
    private RpcSerializationTransformer transformer=new RpcSerializationTransformer();
    public static final int DEFAULT_PORT = 4560;
    public static final int DEFAULT_RECONNECTION_DELAY = 30000;
    public static final int DEFAULT_QUEUE_SIZE = 128;
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;
    private static final int DEFAULT_EVENT_DELAY_TIMEOUT = 100;
    private String remoteHost;
    private int port = 4560;
    private InetAddress address;
    private Duration reconnectionDelay = new Duration(30000L);
    private int queueSize = 128;
    private int acceptConnectionTimeout = 5000;
    private Duration eventDelayLimit = new Duration(100L);
    private BlockingQueue<ILoggingEvent> queue;
    private String peerId;
    private Future<?> task;
    private Future<Socket> connectorTask;
    private volatile Socket socket;
    public void start() {
        if(!this.isStarted()) {
            int errorCount = 0;
            if(this.port <= 0) {
                ++errorCount;
                this.addError("No port was configured for appender" + this.name + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_port");
            }

            if(this.remoteHost == null) {
                ++errorCount;
                this.addError("No remote host was configured for appender" + this.name + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_host");
            }

            if(this.queueSize < 0) {
                ++errorCount;
                this.addError("Queue size must be non-negative");
            }

            if(errorCount == 0) {
                try {
                    this.address = InetAddress.getByName(this.remoteHost);
                } catch (UnknownHostException var3) {
                    this.addError("unknown host: " + this.remoteHost);
                    ++errorCount;
                }
            }

            if(errorCount == 0) {
                this.queue = this.newBlockingQueue(this.queueSize);
                this.peerId = "remote peer " + this.remoteHost + ":" + this.port + ": ";
                this.task = this.getContext().getExecutorService().submit(this);
                super.start();
            }

        }
    }

    public void stop() {
        if(this.isStarted()) {
            CloseUtil.closeQuietly(this.socket);
            this.task.cancel(true);
            if(this.connectorTask != null) {
                this.connectorTask.cancel(true);
            }

            super.stop();
        }
    }

    protected void append(ILoggingEvent event) {
        if(event != null && this.isStarted()) {
            try {
                boolean e = this.queue.offer(event, this.eventDelayLimit.getMilliseconds(), TimeUnit.MILLISECONDS);
                if(!e) {
                    this.addInfo("Dropping event due to timeout limit of [" + this.eventDelayLimit + "] milliseconds being exceeded");
                }
            } catch (InterruptedException var3) {
                this.addError("Interrupted while appending event to SocketAppender", var3);
            }

        }
    }

    public final void run() {
        this.signalEntryInRunMethod();

        try {
            while(!Thread.currentThread().isInterrupted()) {
                SocketConnector ex = this.createConnector(this.address, this.port, 0, this.reconnectionDelay.getMilliseconds());
                this.connectorTask = this.activateConnector(ex);
                if(this.connectorTask == null) {
                    break;
                }

                this.socket = this.waitForConnectorToReturnASocket();
                if(this.socket == null) {
                    break;
                }

                this.dispatchEvents();
            }
        } catch (InterruptedException var2) {
            ;
        }

        this.addInfo("shutting down");
    }

    protected void signalEntryInRunMethod() {
    }

    private SocketConnector createConnector(InetAddress address, int port, int initialDelay, long retryDelay) {
        SocketConnector connector = this.newConnector(address, port, (long)initialDelay, retryDelay);
        connector.setExceptionHandler(this);
        connector.setSocketFactory(this.getSocketFactory());
        return connector;
    }

    private Future<Socket> activateConnector(SocketConnector connector) {
        try {
            return this.getContext().getExecutorService().submit(connector);
        } catch (RejectedExecutionException var3) {
            return null;
        }
    }

    private Socket waitForConnectorToReturnASocket() throws InterruptedException {
        try {
            Socket e = (Socket)this.connectorTask.get();
            this.connectorTask = null;
            return e;
        } catch (ExecutionException var2) {
            return null;
        }
    }

    private void dispatchEvents() throws InterruptedException {
        try {
            this.socket.setSoTimeout(this.acceptConnectionTimeout);
            ObjectOutputStream ex = new ObjectOutputStream(this.socket.getOutputStream());
            this.socket.setSoTimeout(0);
            this.addInfo(this.peerId + "connection established");
            int counter = 0;

            while(true) {
                do {
                    ILoggingEvent event = this.queue.take();
                    this.postProcessEvent(event);
                    Serializable serEvent = this.getPST().transform(event);
                    ex.writeObject(serEvent);
                    ex.flush();
                    ++counter;
                } while(counter < 70);

                ex.reset();
                counter = 0;
            }
        } catch (Throwable var8) {
            var8.printStackTrace();
            this.addInfo(this.peerId + "connection failed: " + var8);
        } finally {
            CloseUtil.closeQuietly(this.socket);
            this.socket = null;
            this.addInfo(this.peerId + "connection closed");
        }

    }

    public void connectionFailed(SocketConnector connector, Exception ex) {
        if(ex instanceof InterruptedException) {
            this.addInfo("connector interrupted");
        } else if(ex instanceof ConnectException) {
            this.addInfo(this.peerId + "connection refused");
        } else {
            this.addInfo(this.peerId + ex);
        }

    }

    protected SocketConnector newConnector(InetAddress address, int port, long initialDelay, long retryDelay) {
        return new DefaultSocketConnector(address, port, initialDelay, retryDelay);
    }

    protected SocketFactory getSocketFactory() {
        return SocketFactory.getDefault();
    }

    BlockingQueue<ILoggingEvent> newBlockingQueue(int queueSize) {
        return (BlockingQueue)(queueSize <= 0?new SynchronousQueue():new ArrayBlockingQueue(queueSize));
    }

    protected void postProcessEvent(ILoggingEvent loggingEvent){
        if(includeCallerData){
            loggingEvent.getCallerData();
        }
    }

    protected PreSerializationTransformer<ILoggingEvent> getPST(){
        return transformer;
    }

    /** @deprecated */
    @Deprecated
    protected static InetAddress getAddressByName(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (Exception var2) {
            return null;
        }
    }

    public void setRemoteHost(String host) {
        this.remoteHost = host;
    }

    public String getRemoteHost() {
        return this.remoteHost;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public void setReconnectionDelay(Duration delay) {
        this.reconnectionDelay = delay;
    }

    public Duration getReconnectionDelay() {
        return this.reconnectionDelay;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getQueueSize() {
        return this.queueSize;
    }

    public void setEventDelayLimit(Duration eventDelayLimit) {
        this.eventDelayLimit = eventDelayLimit;
    }

    public Duration getEventDelayLimit() {
        return this.eventDelayLimit;
    }

    void setAcceptConnectionTimeout(int acceptConnectionTimeout) {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }
}
