package Zeze.Services.ServiceManager;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Util.Task;

public final class AgentClient extends Zeze.Services.HandshakeClient {
	private final Agent agent;
	/**
	 * 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
	 */
	private AsyncSocket Socket;

	public AgentClient(Agent agent, Zeze.Config config) throws Throwable {
		super(Agent.DefaultServiceName, config);
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
		return Socket;
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		if (Socket == null) {
			Socket = sender;
			Task.run(agent::OnConnected, "ServiceManager.Agent.OnConnected");
		} else {
			Agent.logger.error("Has Connected.");
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		if (Socket == so) {
			Socket = null;
		}
		super.OnSocketClose(so, e);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		// ServiceManager的协议处理直接在网络线程中执行。
		Task.Call(() -> factoryHandle.Handle.handle(p), p, (_p, code) -> p.SendResultCode(code));
	}
}
