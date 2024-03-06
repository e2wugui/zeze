package Zeze.Raft;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;

/**
 * 当raft节点运行在一个进程内时，可以通过代理方式复用同一个连接。
 * 这个类在一个进程内只有一个实例，然后传入启用代理的Raft.Agent使用。
 * 另外，ProxyDispatch 的代码也写在这里，只是一个静态函数。
 */
public class ProxyAgent extends Service {
	public static final String eProxyAgentName = "Zeze.Raft.ProxyAgent";
	private final int rpcTimeout;

	public ProxyAgent(int rpcTimeout) {
		super(eProxyAgentName, (Config)null);
		this.rpcTimeout = rpcTimeout;

		AddFactoryHandle(ProxyRequest.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProxyRequest::new,
				this::ProcessProxyRequest,
				TransactionLevel.None
		));
	}

	/**
	 * 把代理请求派发到指定的raft中执行。
	 *
	 * @param r ProxyRequest
	 */
	private long ProcessProxyRequest(ProxyRequest r) throws Exception {
		var agent = agents.get(r.Argument.getRaftName());
		if (null == agent)
			return Procedure.ProviderNotExist;
		var client = agent.getClient();
		var outFactoryHandle = new OutObject<Service.ProtocolFactoryHandle<?>>();
		var p = Protocol.decode(client::findProtocolFactoryHandle, ByteBuffer.Wrap(r.Argument.getRpcBinary()), outFactoryHandle);
		if (null == p)
			return Procedure.NotImplement;

		if (!(p instanceof ProxyableRpc<?, ?>))
			throw new RuntimeException("not a proxyable rpc.");

		var proxyable = (ProxyableRpc<?, ?>)p;
		proxyable.setProxyRequest(r);
		// 重新派发一次，有点浪费线程切换，以后再考虑优化。
		client.dispatchProtocol(p, outFactoryHandle.value);
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
	/**
	 * 获取Leader的ConnectorEx，
	 *
	 * @param node leader node config
	 * @return Agent.ConnectorEx
	 */
	public Agent.ConnectorProxy getLeader(RaftConfig.Node node) {
		if (!node.getProxyHost().isBlank() && node.getProxyPort() != 0) {
			var outConnector = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(
					node.getProxyHost(), node.getProxyPort(),
					true, outConnector, ConnectorEx::new)) {
				outConnector.value.start();
			}
			return ((ConnectorEx)outConnector.value).getConnectorProxy(node.getName());
		}
		return null;
	}

	private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();

	public void addAgent(Agent agent) {
		var raftConfig = agent.getRaftConfig();
		for (var node : raftConfig.getNodes().values()) {
			if (null != agents.putIfAbsent(node.getName(), agent))
				throw new RuntimeException("duplicate agent node " + node.getName());
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
	public static boolean send(Service localService,
							   ProxyAgent proxyAgent,
							   RaftRpc<?, ?> rpc,
							   Agent.ConnectorProxy leader,
							   AsyncSocket leaderSocket) {

		if (null != proxyAgent) {
			if (null != leader) {
				var proxyArgument = new ProxyArgument(leader.getName(), rpc);
				var proxyRpc = new ProxyRequest(proxyArgument);
				// leaderSocket 就是从leader中获取的，这里是为了在循环中发送的时候不用每次获取，优化！
				//logger.info("send to {}", leaderSocket.getRemoteAddress());
				return proxyRpc.Send(leaderSocket, (proxyRpcThis) -> {
					if (proxyRpc.getResultCode() == 0) {
						var outFh = new OutObject<Service.ProtocolFactoryHandle<?>>();
						var resultRpc = Protocol.decode(
								localService::findProtocolFactoryHandle,
								ByteBuffer.Wrap(proxyRpc.Result.getData()),
								outFh);
						if (null != resultRpc && null != rpc.getResponseHandle()) {
							@SuppressWarnings("rawtypes")
							var originHandle = (ProtocolHandle)rpc.getResponseHandle();
							localService.dispatchRpcResponse(resultRpc, originHandle, outFh.value);
						}
					} else {
						logger.error("Agent ProxyRequest({}) error={}",
								proxyArgument.getRaftName(), IModule.getErrorCode(proxyRpc.getResultCode()));
					}
					return 0;
				}, proxyAgent.rpcTimeout);
			}
			// leader 还没有选出。
			return false;
		}
		// 旧的独立的直接的raft访问发送方式。
		return rpc.Send(leaderSocket);
	}
}
