package com.xiaoluo.rpc.boot;

/**
 * @author Chenlong
 * 模块扩展上层抽象类，新增功能模块继承此类作为模块入口以及提供初始化方法
 * */
public abstract class ModuleExtension {
	public abstract void init();
	public void destory(){
	}
}
