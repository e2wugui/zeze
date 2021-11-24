package Zeze.Services.ServiceManager;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;

public final class AgentClient extends Zeze.Services.HandshakeClient {
	private final Agent agent;
	public Agent getAgent() {
		return agent;
	}
	/** 
	 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
	*/
	private AsyncSocket Socket;
	public AsyncSocket getSocket() {
		return Socket;
	}

	public AgentClient(Agent agent, Zeze.Config config) throws Throwable {
		super(Agent.DefaultServiceName, config);
		this.agent = agent;
	}

	public AgentClient(Agent agent, Zeze.Config config, String name) throws Throwable {
		super(name, config);
		this.agent = agent;
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		if (null == Socket) {
			Socket = sender;
			Zeze.Util.Task.Run(agent::OnConnected, "ServiceManager.Agent.OnConnected");
		}
		else {
			Agent.logger.error("Has Connected.");
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
		if (getSocket() == so) {
			Socket = null;
		}
		super.OnSocketClose(so, e);
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) throws Throwable {
		// Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
		if (null != factoryHandle.Handle) {
			agent.getZeze().__GetInternalThreadPoolUnsafe().execute(
					() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p));
		}
	}
}
