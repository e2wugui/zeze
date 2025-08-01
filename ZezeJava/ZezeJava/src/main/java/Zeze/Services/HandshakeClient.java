package Zeze.Services;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(@NotNull String name, @Nullable Config config) {
		super(name, config);
		addHandshakeClientFactoryHandle();
	}

	public HandshakeClient(@NotNull String name, @Nullable Application app) {
		super(name, app);
		addHandshakeClientFactoryHandle();
	}

	@Override
	public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
	}
}
