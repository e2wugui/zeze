package Zeze.Services;

import Zeze.Net.*;
import Zeze.*;

public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(String name, Zeze.Config config) throws Throwable {
		super(name, config);
		AddHandshakeClientFactoryHandle();
	}

	public HandshakeClient(String name, Application app) throws Throwable {
		super(name, app);
		AddHandshakeClientFactoryHandle();
	}


	public final void Connect(String hostNameOrAddress, int port) throws Throwable {
		Connect(hostNameOrAddress, port, true);
	}

	public final void Connect(String hostNameOrAddress, int port, boolean autoReconnect) throws Throwable {
		var c = new Zeze.Util.OutObject<Connector>();
		getConfig().TryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, c);
		c.Value.Start();
	}

	@Override
	public void OnSocketConnected(AsyncSocket so) throws Throwable {
		// 重载这个方法，推迟OnHandshakeDone调用
		SocketMap.putIfAbsent(so.getSessionId(), so);
		StartHandshake(so);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}