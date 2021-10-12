package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.math.*;

public class HandshakeClient extends HandshakeBase {
	public HandshakeClient(String name, Config config) {
		super(name, config);
		AddHandshakeClientFactoryHandle();
	}

	public HandshakeClient(String name, Application app) {
		super(name, app);
		AddHandshakeClientFactoryHandle();
	}


	public final void Connect(String hostNameOrAddress, int port) {
		Connect(hostNameOrAddress, port, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Connect(string hostNameOrAddress, int port, bool autoReconnect = true)
	public final void Connect(String hostNameOrAddress, int port, boolean autoReconnect) {
		var c = new Zeze.Util.OutObject<Connector>();
		getConfig().TryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, c);
		c.Value.Start();
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