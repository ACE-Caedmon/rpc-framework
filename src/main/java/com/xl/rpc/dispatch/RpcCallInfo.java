package com.xl.rpc.dispatch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Caedmon on 2015/10/16.
 */
public class RpcCallInfo {
    private Map<String,LinkedBlockingQueue<RpcCallRecord>> rpcCallRecordMap =new ConcurrentHashMap<>();
    private int maxRecordSize=DEFAULT_MAX_RECORD_SIZE;
    private static final int DEFAULT_MAX_RECORD_SIZE=100;
    private Map<String,AtomicInteger> callTimesMap=new HashMap<>();
    private static final Logger log= LoggerFactory.getLogger(RpcCallInfo.class);
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
        LinkedBlockingQueue<RpcCallRecord> records= rpcCallRecordMap.get(cmd);
        if(records==null){
            records=new LinkedBlockingQueue<>();
            rpcCallRecordMap.put(cmd, records);
        }
        try{
            log.error("Record Size {}", records.size());
            while(records.size()>=maxRecordSize){
                records.poll();
            }
        }catch (NoSuchElementException e){
            e.printStackTrace();
            log.error("Poll record error",e);

        }
        records.add(rpcCallRecord);
    }

    public Map<String, LinkedBlockingQueue<RpcCallRecord>> getRpcCallRecordMap() {
        return rpcCallRecordMap;
    }

    public void setRpcCallRecordMap(Map<String, LinkedBlockingQueue<RpcCallRecord>> rpcCallRecordMap) {
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

    public static void main(String[] args) {
        List<RpcCallRecord> list=new LinkedList<>();
        RpcCallRecord record=new RpcCallRecord();
        record.setCmd("test");
        RpcCallRecord record2=new RpcCallRecord();
        record2.setCmd("test2");
        list.add(record);
        list.add(record2);
        String text= JSON.toJSONString(list,new SerializerFeature[]{SerializerFeature.WriteClassName});
        System.out.println(text);
        LinkedBlockingQueue<RpcCallRecord> result=JSON.parseObject(text, LinkedBlockingQueue.class);
        System.out.println(result.poll().getCmd());
        System.out.println(result.poll().getCmd());
    }
}

