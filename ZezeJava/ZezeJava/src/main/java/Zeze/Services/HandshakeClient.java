package Zeze.Services;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Util.OutObject;

public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(String name, Config config) {
		super(name, config);
		addHandshakeClientFactoryHandle();
	}

	public HandshakeClient(String name, Application app) {
		super(name, app);
		addHandshakeClientFactoryHandle();
	}

	public final void connect(String hostNameOrAddress, int port) {
		connect(hostNameOrAddress, port, true);
	}

	public final void connect(String hostNameOrAddress, int port, boolean autoReconnect) {
		var c = new OutObject<Connector>();
		getConfig().tryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, c);
		c.value.start();
	}

	@Override
	public void OnSocketConnected(AsyncSocket so) {
		// 重载这个方法，推迟OnHandshakeDone调用
		addSocket(so);
	}
}
