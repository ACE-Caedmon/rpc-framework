package com.xl.rpc.monitor.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xl.rpc.cluster.client.ServerNode;
import com.xl.rpc.exception.CannotDelActiveNodeException;
import com.xl.rpc.monitor.MonitorNode;
import com.xl.rpc.monitor.MonitorGroup;
import com.xl.rpc.monitor.event.*;
import com.xl.session.ISession;
import com.xl.utils.Util;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Caedmon on 2015/9/22.
 */
public class MonitorManager {
    public static final String NODE_DATA_FILE_NAME = "node.data";
    public static final String CONFIG_DATA_FILE_NAME = "config.data";
    public static final String CONFIG_BIND_DATA_FILE_NAME = "configBind.data";
    private static final Logger log = LoggerFactory.getLogger(MonitorManager.class);
    private Map<String, MonitorNode> allNodeMap = new ConcurrentHashMap<>();
    private Map<String, String> configMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> configBindMap = new ConcurrentHashMap<>();
    private static final SerializerFeature[] JSON_SERIALIZERFEATURES=new SerializerFeature[]{SerializerFeature.WriteClassName};
    private static final   MonitorManager instance=new MonitorManager();
    private Map<String,MonitorGroup> groupMap=new ConcurrentHashMap<>();
    private volatile boolean started;
    private static final String DATA_DIR="data";
    private final ScheduledExecutorService threadPool= Executors.newScheduledThreadPool(1);
    private static final Object lock=new Object();
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
        threadPool.scheduleAtFixedRate(new MonitorTask(),0,MonitorTask.TIME_OUT_PERIOD, TimeUnit.SECONDS);
        started=true;
        log.info("Init monitor server data");
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
    public MonitorNode register(ISession session,String[] groups, String host, int port) throws Exception{
        String group=groups[0];
        String key=group+"-"+host+":"+port;
        MonitorNode node=allNodeMap.get(key);
        if (node!=null) {
            ISession currentSession=node.getSession();
            if(currentSession!=null&&currentSession.isActive()){
                throw new IllegalStateException("Node exists " + key);
            }else{
                //重连
                node.reconnect(session);
            }
        }else{
            node= MonitorNode.build(session, group, host, port);
            node.setActive(true);
            allNodeMap.put(node.getKey(), node);
            MonitorGroup monitorGroup =groupMap.get(group);
            if(monitorGroup ==null){
                synchronized (lock){
                    if(monitorGroup==null){
                        monitorGroup =new MonitorGroup();
                        monitorGroup.setKey(group);
                    }
                }

            }
            monitorGroup.add(node);
            groupMap.put(group, monitorGroup);
            log.info("Rpc node register success:{}", node.getKey());
        }
        for(MonitorNode n: allNodeMap.values()){
            //不通知自己
            if(n.getSession()==session){
                continue;
            }
            if(!n.isActive()){
                continue;
            }
            NodeActiveEvent event=new NodeActiveEvent();
            event.setNodeKey(node.getKey());
            event.setGroup(node.getGroup());
            event.setHost(node.getHost());
            event.setPort(node.getPort());
            n.notifyEvent(event);
        }
        save(SaveType.NODES);
        return node;
    }

    public MonitorNode delete(String nodeKey) {
        log.info("Delete monitor node:"+nodeKey);
        MonitorNode node=allNodeMap.get(nodeKey);
        if(node==null){
            return null;
        }
        //节点为活动状态不能删除
        if(node.isActive()){
            throw new CannotDelActiveNodeException("The monitor node "+nodeKey+" is active");
        }
        allNodeMap.remove(nodeKey);
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
        return node;
    }
    public void notifyEventToAll(MonitorEvent event){
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
                    text = JSON.toJSONString(configMap,JSON_SERIALIZERFEATURES);
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
        log.info("Monitor server update config:key={},config={}", configKey, configValue);
        Set<String> nodes=configBindMap.get(configKey);
        if(nodes!=null&&!nodes.isEmpty()){
            for(String nodeKey:nodes){
                MonitorNode node= getRpcNode(nodeKey);
                if(node!=null){
                    ConfigEvent event=new ConfigEvent();
                    event.addConfigEntity(configKey,configValue);
                    node.notifyEvent(event);
                }
            }
        }
        save(SaveType.CONFIG);
    }

    public void updateConfigBind(String nodeKey, Set<String> configKeySet) {
        log.info("Update config bind:nodeKey={},configKeySet={}", nodeKey, configKeySet);
        for(String configKey:configKeySet){
            Set<String> bindNodes = configBindMap.get(configKey);
            if (bindNodes == null) {
                bindNodes = new HashSet<>();
                configBindMap.put(configKey, bindNodes);
            }
            bindNodes.add(nodeKey);
        }
        MonitorNode monitorNode=allNodeMap.get(nodeKey);
        monitorNode.setBindConfigKeySet(configKeySet);
        ConfigEvent configEvent=new ConfigEvent();
        for(String configKey:configKeySet){
            String configValue= MonitorManager.getInstance().getConfig(configKey);
            configEvent.addConfigEntity(configKey,configValue);
        }
        monitorNode.notifyEvent(configEvent);
        save(SaveType.CONFIG_BIND);
        save(SaveType.NODES);
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
        log.info("Disconnect monitor node:"+nodeKey);
        MonitorNode node=allNodeMap.get(nodeKey);
        if(null!=node){
            node.setSession(null);
            node.setActive(false);
            node.disconnect();
            NodeInActiveEvent event=new NodeInActiveEvent();
            event.setNodeKey(nodeKey);
            notifyEventToAll(event);
        }
        save(SaveType.NODES);
    }
    public void deleteConfig(String configKey){
        if(configKey!=null){
            log.info("Delete config:{}", configKey);
            Set<String> bindSet=configBindMap.get(configKey);
            if(bindSet==null||bindSet.isEmpty()){
                configMap.remove(configKey);
            }else{
                throw new IllegalStateException("There is no release of configuration binding relationship:{}");
            }
            save(SaveType.CONFIG);
        }


    }

    public void updateRouteTable(String nodeKey,String routeTableString) throws Exception{
        log.info("Update route table:node={},content={}", nodeKey, routeTableString);
        InputStream inputStream=new ByteArrayInputStream(routeTableString.getBytes());
        Properties properties=new Properties();
        properties.load(inputStream);
        inputStream.close();
        MonitorNode node=allNodeMap.get(nodeKey);
        node.setRouteTable(properties);
        RouteEvent event=new RouteEvent();
        event.setRouteTable(properties);
        node.notifyEvent(event);
        save(SaveType.NODES);

    }
    public String showRouteTable(String nodeKey){
        log.info("Show route table:node={}",nodeKey);
        StringBuilder properties=new StringBuilder();
        Properties routeTable=allNodeMap.get(nodeKey).getRouteTable();
        for(Map.Entry<Object,Object> entry:routeTable.entrySet()){
            properties.append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }
        return properties.toString();
    }
    public Map<String,MonitorGroup> getGroupMap(){
        return groupMap;
    }
}
