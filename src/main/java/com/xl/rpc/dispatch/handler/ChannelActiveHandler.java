package com.xl.rpc.dispatch.handler;


import com.xl.rpc.event.IEventHandler;
import com.xl.session.ISession;
import com.xl.session.Session;

public class ChannelActiveHandler implements IEventHandler<ISession> {

	@Override
	public void handleEvent(ISession session) {
		session.setAttribute(Session.NEED_ENCRYPT, false);//开始设置为false
	}

}
