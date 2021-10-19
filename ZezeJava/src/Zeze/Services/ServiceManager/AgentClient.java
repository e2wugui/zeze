package Zeze.Services.ServiceManager;

import Zeze.Net.AsyncSocket;

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

	public AgentClient(Agent agent, Zeze.Config config) {
		super(Agent.DefaultServiceName, config);
		this.agent = agent;
	}

	public AgentClient(Agent agent, Zeze.Config config, String name) {
		super(name, config);
		this.agent = agent;
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) {
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
	public void OnSocketClose(AsyncSocket so, Throwable e) {
		if (getSocket() == so) {
			Socket = null;
		}
		super.OnSocketClose(so, e);
	}
}
