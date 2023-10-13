package Zeze.Raft;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
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
	public final static String eProxyAgentName = "Zeze.Raft.ProxyAgent";

	public ProxyAgent() {
		super(eProxyAgentName, (Config)null);

		AddFactoryHandle(ProxyRequest.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProxyRequest::new,
				this::ProcessProxyRequest,
				TransactionLevel.None
		));
	}

	/**
	 * 把代理请求派发到指定的raft中执行。
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
		var raftRpc = (RaftRpc<?, ?>)p;
		raftRpc.setProxyRequest(r);

		// 重新派发一次，有点浪费线程切换，以后再考虑优化。
		client.dispatchProtocol(p, outFactoryHandle.value);
		return 0;
	}

	/**
	 * 获取Leader的ConnectorEx，
	 * @param node leader node config
	 * @return Agent.ConnectorEx
	 */
	public Agent.ConnectorEx getLeader(RaftConfig.Node node) {
		if (!node.getProxyHost().isBlank() && node.getProxyPort() != 0) {
			var outNew = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(node.getProxyHost(), node.getProxyPort(), true, outNew)) {
				outNew.value.start();
			}
		}
		return null;
	}

	private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();

	public void addAgent(Agent agent) {
		if (null != agents.putIfAbsent(agent.getName(), agent))
			throw new RuntimeException("duplicate agent " + agent.getName());
	}

	/**
	 * 如果启用了代理，则把rpc包装成代理协议，发送出去；
	 * 否则按原始raft请求发送出去。
	 *
	 * @see ProxyServer send
	 * @param proxyAgent 启用代理的实例
	 * @param rpc 发送的rpc
	 * @param leader leader连接器，可能是原始的，也可能是伪造的。可能为null。
	 * @param leaderSocket leader.Socket。可能为null。
	 * @return 发送结果，可能失败。
	 */
	@SuppressWarnings("unchecked")
	public static boolean send(Service localService,
							   ProxyAgent proxyAgent,
							   RaftRpc<?, ?> rpc,
							   Agent.ConnectorEx leader,
							   AsyncSocket leaderSocket) {

		if (null != proxyAgent) {
			if (null != leader) {
				var proxyArgument = new ProxyArgument(leader.getName(), rpc);
				var proxyRpc = new ProxyRequest(proxyArgument);
				// leaderSocket 就是从leader中获取的，这里是为了在循环中发送的时候不用每次获取，优化！
				return proxyRpc.Send(leaderSocket, (proxyRpcThis) -> {
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
					return 0;
				});
			}
			// leader 还没有选出。
			return false;
		}
		// 旧的独立的直接的raft访问发送方式。
		return rpc.Send(leaderSocket);
	}
}
