package Zeze.Services;

import Zeze.Application;
import Zeze.Net.AsyncSocket;
import Zeze.Services.Handshake.SHandshake0;

public class HandshakeServer extends HandshakeBase {
	public HandshakeServer(String name, Zeze.Config config) throws Throwable {
		super(name, config);
		addHandshakeServerFactoryHandle();
	}

	public HandshakeServer(String name, Application app) throws Throwable {
		super(name, app);
		addHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) throws Throwable {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
		var hand0 = new SHandshake0();
		var options = getConfig().getHandshakeOptions();
		hand0.Argument.encryptType = options.getEncryptType();
		hand0.Argument.supportedEncryptList = options.getSupportedEncrypt();
		hand0.Argument.compressS2c = options.getCompressS2c();
		hand0.Argument.compressC2s = options.getCompressC2s();
		hand0.Argument.supportedCompressList = options.getSupportedCompress();
		hand0.Send(so);
	}
}
