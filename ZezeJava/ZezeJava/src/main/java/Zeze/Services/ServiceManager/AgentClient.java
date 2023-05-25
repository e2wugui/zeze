package Zeze.Services.ServiceManager;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class AgentClient extends HandshakeClient {
	private final Agent agent;
	/**
	 * 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
	 */
	private AsyncSocket socket;

	public AgentClient(Agent agent, Config config) {
		super(Agent.defaultServiceName, config);
		this.agent = agent;
	}

	public AgentClient(Agent agent, Config config, String name) {
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
			Task.runUnsafe(agent::onConnected, "ServiceManager.AgentClient.OnHandshakeDone", DispatchMode.Normal);
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
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
		// 不支持事务
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// 不支持事务
		Task.runUnsafe(() -> p.handle(this, factoryHandle),
				p, Protocol::trySendResultCode, null, factoryHandle.Mode);
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// 不支持事务
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}
}
