package com.xl.rpc.dispatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Caedmon on 2015/10/16.
 */
public class RpcCallInfo {
    private Map<String,LinkedList<RpcCallRecord>> rpcCallRecordMap =new HashMap<>();
    private int maxRecordSize=DEFAULT_MAX_RECORD_SIZE;
    private static final int DEFAULT_MAX_RECORD_SIZE=100;
    private Map<String,AtomicInteger> callTimesMap=new HashMap<>();
    public RpcCallInfo(int maxRecordSize){
        this.maxRecordSize=maxRecordSize;
    }
    public RpcCallInfo(){
        this(DEFAULT_MAX_RECORD_SIZE);
    }
    public void addRecord(String cmd, long callTime,long cost){
        //记录调用次数
        AtomicInteger cmdCallTimes=callTimesMap.get(cmd);
        if(cmdCallTimes==null){
            cmdCallTimes=new AtomicInteger(1);
            callTimesMap.put(cmd,cmdCallTimes);
        }else{
            cmdCallTimes.incrementAndGet();
        }
        //保存调用记录详情
        RpcCallRecord rpcCallRecord=new RpcCallRecord();
        rpcCallRecord.setCmd(cmd);
        rpcCallRecord.setCost(cost);
        rpcCallRecord.setCallTime(callTime);
        LinkedList<RpcCallRecord> records= rpcCallRecordMap.get(cmd);
        if(records==null){
            records=new LinkedList<>();
            rpcCallRecordMap.put(cmd, records);
        }
        while(records.size()>=maxRecordSize){
            records.removeFirst();
        }
        records.add(rpcCallRecord);
    }

    public Map<String, LinkedList<RpcCallRecord>> getRpcCallRecordMap() {
        return rpcCallRecordMap;
    }

    public void setRpcCallRecordMap(Map<String, LinkedList<RpcCallRecord>> rpcCallRecordMap) {
        this.rpcCallRecordMap = rpcCallRecordMap;
    }

    public int getMaxRecordSize() {
        return maxRecordSize;
    }

    public void setMaxRecordSize(int maxRecordSize) {
        this.maxRecordSize = maxRecordSize;
    }

    public Map<String, AtomicInteger> getCallTimesMap() {
        return callTimesMap;
    }

    public void setCallTimesMap(Map<String, AtomicInteger> callTimesMap) {
        this.callTimesMap = callTimesMap;
    }

    public int getTotalCallTimes(){
        Collection<AtomicInteger> callTimes=callTimesMap.values();
        int result=0;
        for(AtomicInteger cmdCallTime:callTimes){
            result=result+cmdCallTime.get();
        }
        return result;
    }
    public int getCmdCallTimes(String cmd){
        AtomicInteger cmdCallTimes=callTimesMap.get(cmd);
        return cmdCallTimes==null?0:cmdCallTimes.get();
    }
    public static class RpcCallRecord {
        private String cmd;
        private long cost;
        private long callTime;
        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }

        public long getCost() {
            return cost;
        }

        public void setCost(long cost) {
            this.cost = cost;
        }

        public long getCallTime() {
            return callTime;
        }

        public void setCallTime(long callTime) {
            this.callTime = callTime;
        }
    }

}

