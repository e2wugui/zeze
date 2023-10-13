package Zeze.Raft;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;

public class ProxyServer extends Service {
	public static final String eProxyServerName = "Zeze.Raft.ProxyServer";

	public ProxyServer(Config config) {
		super(eProxyServerName, config);

		RegisterProtocols();
	}

	public ProxyServer(Application zeze) {
		super(eProxyServerName, zeze);

		RegisterProtocols();
	}

	private void RegisterProtocols() {
		AddFactoryHandle(ProxyRequest.TypeId_, new ProtocolFactoryHandle<>(
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
		var raft = rafts.get(r.Argument.getRaftName());
		if (null == raft)
			return Procedure.ProviderNotExist;
		var server = raft.getServer();
		var outFactoryHandle = new OutObject<ProtocolFactoryHandle<?>>();
		var p = Protocol.decode(server::findProtocolFactoryHandle, ByteBuffer.Wrap(r.Argument.getRpcBinary()), outFactoryHandle);
		if (null == p)
			return Procedure.NotImplement;
		var raftRpc = (RaftRpc<?, ?>)p;
		raftRpc.setProxyRequest(r);

		// 下面的流程从Raft.Server.dispatchProtocol的部分代码拷贝出，请参考原来的地方，进行比较。。
		if (raft.isWorkingLeader()) {
			if (raftRpc.getUnique().getRequestId() <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return 0;
			}

			server.dispatchRaftRequest(p,
					() -> server.processRequest(p, outFactoryHandle.value),
					p.getClass().getName(),
					() -> p.SendResultCode(Procedure.RaftRetry),
					outFactoryHandle.value.Mode);
		}

		// else 如果不是leader，不处理请求，也不发送rpc的正常结果，以后Raft.Agent会resend。
		// 但是会尝试报告一次真正的leader。
		server.trySendLeaderIs(r.getSender());

		return 0;
	}

	private final ConcurrentHashMap<String, Raft> rafts = new ConcurrentHashMap<>();

	public void addRaft(Raft raft) {
		if (null != rafts.putIfAbsent(raft.getName(), raft))
			throw new RuntimeException("duplicate raft " + raft.getName());
		raft.getServer().setProxyServer(this);
	}

	/**
	 * 如果启用了代理，则把rpc包装成代理协议，发送出去；
	 * 否则按原始raft请求发送出去。
	 *
	 * @see ProxyAgent send
	 * @param proxyServer 启用代理的服务器
	 * @param rpc 待发送rpc
	 * @param sender 连接
	 */
	public static boolean send(Service localService, ProxyServer proxyServer, Rpc<?, ?> rpc, String raftName, AsyncSocket sender) {
		if (null == sender)
			return false; // 没有连接，直接失败。

		if (null != proxyServer) {
			var proxyArgument = new ProxyArgument(raftName, rpc);
			var proxyRpc = new ProxyRequest(proxyArgument);
			return proxyRpc.Send(sender, (proxyRpcThis) -> {
				var outFh = new OutObject<ProtocolFactoryHandle<?>>();
				var resultRpc = Protocol.decode(
						localService::findProtocolFactoryHandle,
						ByteBuffer.Wrap(proxyRpc.Result.getData()),
						outFh);
				if (null != resultRpc && null != rpc.getResponseHandle()) {
					@SuppressWarnings("rawtypes")
					var originHandle = (ProtocolHandle)rpc.getResponseHandle();
					//noinspection unchecked
					localService.dispatchRpcResponse(resultRpc, originHandle, outFh.value);
				}
				return 0;
			});
		}

		// 旧的独立的直接的raft访问发送方式。
		return rpc.Send(sender); // ignore response
	}
}
