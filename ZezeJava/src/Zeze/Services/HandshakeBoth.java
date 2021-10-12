package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.math.*;

public class HandshakeBoth extends HandshakeBase {
	public HandshakeBoth(String name, Config config) {
		super(name, config);
		AddHandshakeClientFactoryHandle();
		AddHandshakeServerFactoryHandle();
	}

	public HandshakeBoth(String name, Application app) {
		super(name, app);
		AddHandshakeClientFactoryHandle();
		AddHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		SocketMap.putIfAbsent(so.getSessionId(), so);
	}

	@Override
	public void OnSocketConnected(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		SocketMap.putIfAbsent(so.getSessionId(), so);
		StartHandshake(so);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// 防止Client不进入加密，直接发送用户协议。
		if (false == IsHandshakeProtocol(p.getTypeId())) {
			p.Sender.VerifySecurity();
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}