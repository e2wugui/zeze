package Zeze.Services.ServiceManager;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;

public final class AgentClient extends Zeze.Services.HandshakeClient {
	private final Agent agent;
	/**
	 * 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
	 */
	private AsyncSocket socket;

	public AgentClient(Agent agent, Zeze.Config config) {
		super(Agent.defaultServiceName, config);
		this.agent = agent;
	}

	public AgentClient(Agent agent, Zeze.Config config, String name) {
		super(name, config);
		this.agent = agent;
	}

	public Agent getAgent() {
		return agent;
	}

	public AsyncSocket getSocket() {
		return socket;
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		var firstConnected = socket == null;
		if (firstConnected)
			socket = so; // 下面这行可能导致等待future的其它线程开始执行,所以先给socket赋值
		super.OnHandshakeDone(so);
		if (firstConnected) {
			Task.runUnsafe(agent::onConnected, "ServiceManager.Agent.OnConnected", DispatchMode.Normal);
		} else {
			Agent.logger.error("Has Connected.");
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
		if (socket == so) {
			socket = null;
		}
		super.OnSocketClose(so, e);
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		Task.call(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode);
	}
}
