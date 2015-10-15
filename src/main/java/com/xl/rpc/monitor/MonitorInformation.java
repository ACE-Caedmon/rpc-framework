package com.xl.rpc.monitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Caedmon on 2015/9/17.
 */
public class MonitorInformation {
    private long callTimes;
    private Map<String,List<Long>> rentlyCallRecordMap=new HashMap<>();
    public long getCallTimes() {
        return callTimes;
    }

    public void setCallTimes(long callTimes) {
        this.callTimes = callTimes;
    }

    public Map<String, List<Long>> getRentlyCallRecordMap() {
        return rentlyCallRecordMap;
    }

    public void setRentlyCallRecordMap(Map<String, List<Long>> rentlyCallRecordMap) {
        this.rentlyCallRecordMap = rentlyCallRecordMap;
    }
}
