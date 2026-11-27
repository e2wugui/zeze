package Zeze.Raft;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;
import org.jspecify.annotations.Nullable;
import Zeze.Raft.Agent.ConnectorProxy;

/**
 * Raft 客户端代理服务：进程内多个 Raft.Agent 共用同一实例以复用网络连接。
 */
public class ProxyAgent extends Service {
	public static final String eProxyAgentName = "Zeze.Raft.ProxyAgent";
	private final int rpcTimeout;

	public ProxyAgent(int rpcTimeout) {
		super(eProxyAgentName, (Config)null);
		this.rpcTimeout = rpcTimeout;
		getSocketOptions().setOutputBufferMaxSize(100 * 1024 * 1024);
		getSocketOptions().setInputBufferMaxProtocolSize(100 * 1024 * 1024);

		AddFactoryHandle(ProxyRequest.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProxyRequest::new,
				this::ProcessProxyRequest,
				TransactionLevel.None
		));
	}

	/** 把代理请求派发到目标 Agent 的客户端中执行。 */
	private long ProcessProxyRequest(ProxyRequest r) throws Exception {
		var agent = agents.get(r.Argument.getRaftName());
		if (null == agent)
			return Procedure.ProviderNotExist;
		var client = agent.getClient();
		if (null == client)
			return Procedure.ProviderNotExist;
		var outFactoryHandle = new OutObject<Service.ProtocolFactoryHandle<?>>();
		var p = Protocol.decode(client::findProtocolFactoryHandle, ByteBuffer.Wrap(r.Argument.getRpcBinary()), outFactoryHandle);
		if (null == p)
			return Procedure.NotImplement;

		if (!(p instanceof ProxyableRpc<?, ?> proxyable))
			throw new RuntimeException("not a proxyable rpc.");

		proxyable.setProxyRequest(r);
		// 重新派发一次，有点浪费线程切换，以后再考虑优化。
		client.dispatchProtocol(p, Objects.requireNonNull(outFactoryHandle.value));
		return 0;
	}

	public static class ConnectorEx extends Connector {
		private final ConcurrentHashMap<String, Agent.ConnectorProxy> proxys = new ConcurrentHashMap<>();

		public ConnectorEx(String host, int port) {
			super(host, port, true);
		}

		public ConnectorEx(String host, int port, boolean autoConnect) {
			super(host, port, autoConnect);
		}

		public Agent.ConnectorProxy getConnectorProxy(String name) {
			return proxys.computeIfAbsent(name, __ -> new Agent.ConnectorProxy(name, this));
		}
	}
	/** 获取目标 Leader 节点对应的代理连接器。 */
	public @Nullable ConnectorProxy getLeader(RaftConfig.Node node) {
		if (!node.getProxyHost().isBlank() && node.getProxyPort() != 0) {
			var outConnector = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(
					node.getProxyHost(), node.getProxyPort(),
					true, outConnector, ConnectorEx::new)) {
				//noinspection DataFlowIssue
				outConnector.value.start();
			}
			//noinspection DataFlowIssue
			return ((ConnectorEx)outConnector.value).getConnectorProxy(node.getName());
		}
		return null;
	}

	private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();

	// 首发现整轮无应答后的共享退避窗口：短时间内跳过后续 Agent 的发现轮，避免连续空耗超时预算；
	// 有应答但无 Leader（如选举中）不触发免疫，收到权威应答后立即清除。
	static final long discoverNoAnswerImmunityMs = 5000;
	private volatile long discoverImmunityUntil;

	boolean isDiscoverImmune() {
		return System.currentTimeMillis() < discoverImmunityUntil;
	}

	void markDiscoverNoAnswer() {
		discoverImmunityUntil = System.currentTimeMillis() + discoverNoAnswerImmunityMs;
	}

	void clearDiscoverImmunity() {
		discoverImmunityUntil = 0;
	}

	public void addAgent(Agent agent) {
		var nodeNames = List.copyOf(agent.getRaftConfig().getNodes().keySet());
		synchronized (agents) {
			for (var nodeName : nodeNames) {
				if (agents.containsKey(nodeName))
					throw new RuntimeException("duplicate agent node " + nodeName);
			}
			for (var nodeName : nodeNames)
				agents.put(nodeName, agent);
		}
	}

	/** 注销属于该 Agent 的节点映射（按引用匹配，不误删后注册的新实例）。 */
	public void removeAgent(Agent agent) {
		synchronized (agents) {
			for (var nodeName : agent.getRaftConfig().getNodes().keySet())
				agents.remove(nodeName, agent);
		}
	}

	/**
	 * 如果启用了代理，则把rpc包装成代理协议，发送出去；
	 * 否则按原始raft请求发送出去。
	 *
	 * @param proxyAgent   启用代理的实例
	 * @param rpc          发送的rpc
	 * @param leader       leader连接器，可能是原始的，也可能是伪造的。可能为null。
	 * @param leaderSocket leader.Socket。可能为null。
	 * @return 发送结果，可能失败。
	 * @see ProxyServer send
	 */
	@SuppressWarnings("unchecked")
	public static boolean send(@Nullable Service localService,
							   @Nullable ProxyAgent proxyAgent,
							   RaftRpc<?, ?> rpc,
							   Agent.@Nullable ConnectorProxy leader,
							   @Nullable AsyncSocket leaderSocket) {

		if (null != proxyAgent) {
			if (null != leader) {
				var proxyArgument = new ProxyArgument(leader.getName(), rpc);
				var proxyRpc = new ProxyRequest(proxyArgument);
				// 复用外层已获取的 leaderSocket，避免循环发送时重复查找。
				return proxyRpc.Send(leaderSocket, (proxyRpcThis) -> {
					if (proxyRpc.getResultCode() == 0) {
						var outFh = new OutObject<Service.ProtocolFactoryHandle<?>>();
						//noinspection DataFlowIssue
						var resultRpc = (Rpc<?, ?>)Protocol.decode(
								localService::findProtocolFactoryHandle,
								ByteBuffer.Wrap(proxyRpc.Result.getData()),
								outFh);
						if (null != resultRpc) {
							if (null != rpc.getResponseHandle()) {
								@SuppressWarnings("rawtypes")
								var originHandle = (ProtocolHandle)rpc.getResponseHandle();
								localService.dispatchRpcResponse(resultRpc, originHandle, Objects.requireNonNull(outFh.value));
							} else if (rpc.getFuture() != null){
								// 将 ResultCode 与 Result 拷回原始 rpc 后再完成 future，保留语义码。
								rpc.setResultCode(resultRpc.getResultCode());
								//noinspection UnnecessaryLocalVariable,rawtypes
								Rpc rawRpc = rpc, rawResultRpc = resultRpc;
								rawRpc.Result = rawResultRpc.Result;
								rpc.getFuture().setRawResult(rpc);
							}
						} else {
							logger.error("Agent ProxyRequest({}) resultRpc not found.", proxyArgument.getRaftName());
						}
					} else {
						if (Procedure.Timeout != proxyRpc.getResultCode())
							logger.error("Agent ProxyRequest({}) error={}",
									proxyArgument.getRaftName(), IModule.getErrorCode(proxyRpc.getResultCode()));
						else
							logger.info("Agent ProxyRequest({}) timeout.", proxyArgument.getRaftName());
					}
					return 0;
				}, proxyAgent.rpcTimeout);
			}
			return false;
		}
		return rpc.Send(leaderSocket);
	}
}
