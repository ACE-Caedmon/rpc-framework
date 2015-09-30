package com.xl.rpc.monitor.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.rpc.monitor.MonitorGroup;
import com.xl.rpc.monitor.event.ConfigEvent;
import com.xl.rpc.monitor.event.NodeActiveEvent;
import com.xl.rpc.monitor.event.NodeEvent;
import com.xl.rpc.monitor.event.NodeInActiveEvent;
import com.xl.session.ISession;
import com.xl.utils.Util;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/9/22.
 */
public class MonitorManager {
    public static final AttributeKey<String> RPC_NODE_ATTR_KEY = new AttributeKey<>("RPC_NODE_ATTR_KEY");
    public static final String NODE_DATA_FILE_NAME = "node.data";
    public static final String CONFIG_DATA_FILE_NAME = "config.data";
    public static final String CONFIG_BIND_DATA_FILE_NAME = "configBind.data";
    private static final Logger log = LoggerFactory.getLogger(MonitorManager.class);
    private Map<String, MonitorNode> allNodeMap = new ConcurrentHashMap<>();
    private Map<String, String> configMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> configBindMap = new ConcurrentHashMap<>();
    private static final SerializerFeature[] JSON_SERIALIZERFEATURES=new SerializerFeature[]{SerializerFeature.WriteClassName};
    private static final MonitorManager instance=new MonitorManager();
    private Map<String,MonitorGroup> groupMap=new ConcurrentHashMap<>();
    private volatile boolean started;
    private static final String DATA_DIR="data";
    private enum SaveType{
        NODES,CONFIG,CONFIG_BIND;
    }
    public static MonitorManager getInstance(){
        return instance;
    }
    private MonitorManager(){
        if(started){
            return;
        }
        String nodesText=null;
        String configText=null;
        String configBindText=null;
        try{
            Util.ifNotExistCreate(DATA_DIR+ File.separator + NODE_DATA_FILE_NAME);
            Util.ifNotExistCreate(DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);
            Util.ifNotExistCreate(DATA_DIR + File.separator + CONFIG_BIND_DATA_FILE_NAME);
            nodesText=Util.loadFromDisk(DATA_DIR + File.separator + NODE_DATA_FILE_NAME);
            configText =Util.loadFromDisk(DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);
            configBindText=Util.loadFromDisk(DATA_DIR + File.separator + CONFIG_BIND_DATA_FILE_NAME);
        }catch (IOException e){
            throw new IllegalStateException("MonitorManager init error",e);
        }
        if(nodesText!=null&&!nodesText.isEmpty()){
            this.groupMap=JSON.parseObject(nodesText, ConcurrentHashMap.class);
            for(MonitorGroup group:groupMap.values()){
                for(MonitorNode node:group.getNodes().values()){
                    allNodeMap.put(node.getKey(),node);
                }
            }
        }
        if(configText!=null&&!configText.isEmpty()){
            this.configMap = JSON.parseObject(configText, ConcurrentHashMap.class);
        }
        if(configBindText!=null&&!configBindText.isEmpty()){
            this.configBindMap = JSON.parseObject(configBindText, ConcurrentHashMap.class);
        }
        //先把所有节点状态重置为false
        for(MonitorNode node:allNodeMap.values()){
            node.setActive(false);
        }
        started=true;
    }

    public Map<String, MonitorNode> getAllNodeMap() {
        return allNodeMap;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public Map<String, Set<String>> getConfigBindMap() {
        return configBindMap;
    }

    public void register(ISession session,String[] groups, String host, int port) throws Exception{

        for(String group:groups){
            String key=group+"-"+host+":"+port;
            MonitorNode node=allNodeMap.get(key);
            if (node!=null) {
                ISession currentSession=node.getSession();
                if(currentSession!=null&&currentSession.isActive()){
                    throw new IllegalStateException("Node exists " + key);
                }
            }
            node= MonitorNode.build(session, group, host, port);
            node.setActive(true);
            allNodeMap.put(node.getKey(), node);
            MonitorGroup monitorGroup =groupMap.get(group);
            if(monitorGroup ==null){
                monitorGroup =new MonitorGroup();
                monitorGroup.setKey(group);
            }
            monitorGroup.add(node);
            groupMap.put(group, monitorGroup);
            log.info("Rpc node register success:{}",node.getKey());
        }
        save(SaveType.NODES);
        for(MonitorNode n: allNodeMap.values()){
            //不通知自己
            if(n.getSession()==session){
                continue;
            }
            NodeActiveEvent event=new NodeActiveEvent();
            event.setNodeKey(n.getKey());
            event.setGroup(n.getGroup());
            event.setHost(n.getHost());
            event.setPort(port);
            n.notifyEvent(event);
        }


    }

    public void delete(String nodeKey) {
        MonitorNode node=allNodeMap.remove(nodeKey);
        if(node!=null){
            String group=node.getGroup();
            MonitorGroup monitorGroup=groupMap.get(group);
            if(monitorGroup!=null){
                monitorGroup.delete(nodeKey);
            }
        }
        NodeInActiveEvent event=new NodeInActiveEvent();
        event.setNodeKey(nodeKey);
        notifyEventToAll(event);
        save(SaveType.NODES);
    }
    public void notifyEventToAll(NodeEvent event){
        for(MonitorNode n:allNodeMap.values()){
            n.notifyEvent(event);
        }
    }
    public void save(SaveType type){
        String text =null;
        try{
            switch (type){
                case NODES:
                    text=JSON.toJSONString(groupMap, JSON_SERIALIZERFEATURES);
                    Util.saveToDisk(text, DATA_DIR + File.separator + NODE_DATA_FILE_NAME);
                    break;
                case CONFIG:
                    text = JSON.toJSONString(allNodeMap,JSON_SERIALIZERFEATURES);
                    Util.saveToDisk(text, DATA_DIR + File.separator + CONFIG_DATA_FILE_NAME);

                    break;
                case CONFIG_BIND:
                    text = JSON.toJSONString(configBindMap,JSON_SERIALIZERFEATURES);
                    Util.saveToDisk(text, DATA_DIR + File.separator + CONFIG_BIND_DATA_FILE_NAME);
                    break;
                default:
                    break;
            }
            log.debug("Save {} data to disk success:{}",type, text);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public Collection<MonitorNode> getAllNodeList() {
        return allNodeMap.values();
    }

    public MonitorNode getRpcNode(String nodeKey) {
        return allNodeMap.get(nodeKey);
    }

    public void updateConfig(String configKey, String configValue) {
        configMap.put(configKey, configValue);
        log.info("Update config:key={},config={}", configKey, configValue);
        Set<String> nodes=configBindMap.get(configKey);
        if(nodes!=null&&!nodes.isEmpty()){
            for(String nodeKey:nodes){
                MonitorNode node= getRpcNode(nodeKey);
                if(node!=null){
                    ConfigEvent event=new ConfigEvent();
                    event.setConfigKey(configKey);
                    event.setConfigValue(configValue);
                    node.notifyEvent(event);
                }
            }
        }
        save(SaveType.CONFIG);
    }

    public void updateConfigBind(String nodeKey, String configKey) {
        Set<String> bindNodes = configBindMap.get(configKey);
        if (bindNodes == null) {
            bindNodes = new HashSet<>();
            configBindMap.put(configKey, bindNodes);
        }
        bindNodes.add(nodeKey);
        save(SaveType.CONFIG_BIND);
    }

    public String getConfig(String configKey) {
        return configMap.get(configKey);
    }

    public Map<String, String> getConfigMapByKeys(Set<String> configKeySet) {
        Map<String, String> result = new HashMap<>();
        for (String key : configKeySet) {
            String configValue = getConfig(key);
            result.put(key, configValue);
        }
        return result;
    }

    public Map<String, String> getNodeConfig(String nodeKey) {
        MonitorNode monitorNode = getRpcNode(nodeKey);
        if (monitorNode != null) {
            Set<String> keySet = monitorNode.getBindConfigKeySet();
            return getConfigMapByKeys(keySet);
        }
        return null;
    }
    public void disconnectNode(String nodeKey){
        MonitorNode node=allNodeMap.get(nodeKey);
        if(null!=node){
            node.setSession(null);
            node.setActive(false);
        }
        save(SaveType.NODES);
    }
    public Map<String,MonitorGroup> getGroupMap(){
        return groupMap;
    }
}
