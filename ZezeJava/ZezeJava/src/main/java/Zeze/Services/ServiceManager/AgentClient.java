package Zeze.Services.ServiceManager;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;

public final class AgentClient extends Zeze.Services.HandshakeClient {
	private final Agent agent;
	/**
	 * 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
	 */
	private AsyncSocket socket;

	public AgentClient(Agent agent, Zeze.Config config) throws Throwable {
		super(Agent.defaultServiceName, config);
		this.agent = agent;
	}

	public AgentClient(Agent agent, Zeze.Config config, String name) throws Throwable {
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
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		if (socket == null) {
			socket = sender;
			Task.runUnsafe(agent::onConnected, "ServiceManager.Agent.OnConnected", DispatchMode.Normal);
		} else {
			Agent.logger.error("Has Connected.");
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		if (socket == so) {
			socket = null;
		}
		super.OnSocketClose(so, e);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		Task.call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode);
	}
}
