package Zeze.Services;

import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Services.Handshake.SHandshake0;

public class HandshakeBoth extends HandshakeBase {
	public HandshakeBoth(String name, Zeze.Config config) throws Throwable {
		super(name, config);
		addHandshakeClientFactoryHandle();
		addHandshakeServerFactoryHandle();
	}

	public HandshakeBoth(String name, Application app) throws Throwable {
		super(name, app);
		addHandshakeClientFactoryHandle();
		addHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);

		var hand0 = new SHandshake0();
		hand0.Argument.enableEncrypt = getConfig().getHandshakeOptions().getEnableEncrypt();
		hand0.Send(so);
	}

	@Override
	public void OnSocketConnected(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
	}
}
