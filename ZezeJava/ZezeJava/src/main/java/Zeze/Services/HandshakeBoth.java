package Zeze.Services;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Services.Handshake.SHandshake0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakeBoth extends HandshakeBase {
	public HandshakeBoth(@NotNull String name, @Nullable Config config) {
		super(name, config);
		addHandshakeClientFactoryHandle();
		addHandshakeServerFactoryHandle();
	}

	public HandshakeBoth(@NotNull String name, @NotNull Application app) {
		super(name, app);
		addHandshakeClientFactoryHandle();
		addHandshakeServerFactoryHandle();
	}

	@Override
	public void OnSocketAccept(@NotNull AsyncSocket so) {
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

	@Override
	public void OnSocketConnected(@NotNull AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
	}
}
