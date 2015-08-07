package com.xl.rpc.dispatch.handler;


import com.xl.rpc.event.IEventHandler;
import com.xl.session.ISession;

/**
 * 业务逻辑登录成功后要触发此事件，同步将密码表发送给客户端
 * @author Chenlong
 * */
public class ValidateOKHandler implements IEventHandler<ISession> {
	@Override
	public void handleEvent(ISession session) {
//		List<Short> passports= BinaryEncryptUtil.getPassBody();
//		session.setAttribute(Session.PASSPORT, passports);
//		PassportTable passportMessage=new PassportTable(passports);
//		Future<?> future=session.rpcSend(-1, MsgType.Binary, passportMessage);
//		try {
//			future.get();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		session.setAttribute(Session.NEED_ENCRYPT, true);
		
	}

 
}
