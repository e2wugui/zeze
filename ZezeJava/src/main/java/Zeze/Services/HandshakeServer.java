package Zeze.Services;

import Zeze.Net.*;
import Zeze.*;

public class HandshakeServer extends HandshakeBase {
	public HandshakeServer(String name, Zeze.Config config) throws Throwable {
		super(name, config);
		AddHandshakeServerFactoryHandle();
	}

	public HandshakeServer(String name, Application app) throws Throwable {
		super(name, app);
		AddHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) throws Throwable{
		// 重载这个方法，推迟OnHandshakeDone调用
		SocketMap.putIfAbsent(so.getSessionId(), so);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) throws Throwable{
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}