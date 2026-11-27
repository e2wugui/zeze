package Zeze.Raft;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;

/**
 * Raft 代理服务端：进程内多个 raft 共用一个对外 Service，按 raftName 把代理请求派发到对应的 Raft 执行。
 */
public class ProxyServer extends Service {
	public static final String eProxyServerName = "Zeze.Raft.ProxyServer";
	private final int rpcTimeout;

	public ProxyServer(Config config, int rpcTimeout) {
		super(eProxyServerName, config);
		this.rpcTimeout = rpcTimeout;
		getSocketOptions().setOutputBufferMaxSize(100 * 1024 * 1024);
		getSocketOptions().setInputBufferMaxProtocolSize(100 * 1024 * 1024);
		RegisterProtocols();
	}

	public ProxyServer(Application zeze, int rpcTimeout) {
		super(eProxyServerName, zeze);
		this.rpcTimeout = rpcTimeout;
		getSocketOptions().setOutputBufferMaxSize(100 * 1024 * 1024);
		getSocketOptions().setInputBufferMaxProtocolSize(100 * 1024 * 1024);
		RegisterProtocols();
	}

	private void RegisterProtocols() {
		AddFactoryHandle(ProxyRequest.TypeId_, new ProtocolFactoryHandle<>(
				ProxyRequest::new,
				this::ProcessProxyRequest,
				TransactionLevel.None
		));
	}

	/** 把代理请求派发到目标 Raft 实例执行。 */
	private long ProcessProxyRequest(ProxyRequest r) throws Exception {
		var raft = rafts.get(r.Argument.getRaftName());
		if (null == raft) {
			logger.warn("ProxyRequest: not found raftName={}, available={}", r.Argument.getRaftName(), rafts.keySet());
			return Procedure.ProviderNotExist;
		}
		var server = raft.getServer();
		var outFactoryHandle = new OutObject<ProtocolFactoryHandle<?>>();
		var p = Protocol.decode(server::findProtocolFactoryHandle, ByteBuffer.Wrap(r.Argument.getRpcBinary()), outFactoryHandle);
		if (null == p)
			return Procedure.NotImplement;
		if (!(p instanceof RaftRpc<?, ?> raftRpc) || !raftRpc.isRequest()) {
			logger.warn("ProxyRequest: invalid inner protocol raftName={} protocol={}", r.Argument.getRaftName(), p);
			return Procedure.LogicError;
		}
		raftRpc.setProxyRequest(r);

		// 与 Server.dispatchProtocol 对齐：以下控制类请求无需 Leader 身份且不填 createTime，提前派发以跳过 Leader 门槛与去重校验。
		var pTypeId = p.getTypeId();
		if (pTypeId == GetLeader.TypeId_ || pTypeId == StartServerConnector.TypeId_ || pTypeId == StopServerConnector.TypeId_) {
			server.dispatchRaftRequest(p, () -> TaskSpec.ofFunc(() -> p.handle(server, Objects.requireNonNull(outFactoryHandle.value)),
							p, (__, code) -> p.trySendResultCode(code)).call(),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), outFactoryHandle.value.Mode);
			return 0;
		}

		// 以下流程与 Server.dispatchProtocol 保持一致。
		if (raft.isWorkingLeader()) {
			if (raftRpc.getUnique().getRequestId() <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return 0;
			}

			//noinspection DataFlowIssue
			server.dispatchRaftRequest(p,
					() -> server.processRequest(p, Objects.requireNonNull(outFactoryHandle.value)),
					p.getClass().getName(),
					() -> p.SendResultCode(Procedure.RaftRetry),
					outFactoryHandle.value.Mode);
			return 0;
		}

		// 非 Leader 不处理请求（由 Agent 重发），尝试通告当前 Leader。
		server.trySendLeaderIs(r.getSender());

		return 0;
	}

	private final ConcurrentHashMap<String, Raft> rafts = new ConcurrentHashMap<>();

	public void addRaft(Raft raft) {
		if (null != rafts.putIfAbsent(raft.getName(), raft))
			throw new RuntimeException("duplicate raft " + raft.getName());
		raft.getServer().setProxyServer(this);
	}

	/** 注销指定的 Raft 实例，并按引用匹配清除其 proxyServer 引用。 */
	public void removeRaft(Raft raft) {
		if (rafts.remove(raft.getName(), raft))
			raft.getServer().setProxyServer(null);
	}

	/**
	 * 如果启用了代理，则把rpc包装成代理协议，发送出去；
	 * 否则按原始raft请求发送出去。
	 *
	 * @param proxyServer 启用代理的服务器
	 * @param rpc         待发送rpc
	 * @param sender      连接
	 * @see ProxyAgent send
	 */
	@SuppressWarnings("unchecked")
	public static boolean send(Service localService, @Nullable ProxyServer proxyServer, Rpc<?, ?> rpc, String raftName, @Nullable AsyncSocket sender) {
		if (null == sender)
			return false;

		if (null != proxyServer) {
			var proxyArgument = new ProxyArgument(raftName, rpc);
			var proxyRpc = new ProxyRequest(proxyArgument);
			return proxyRpc.Send(sender, (proxyRpcThis) -> {
				if (proxyRpc.getResultCode() == 0) {
					var outFh = new OutObject<ProtocolFactoryHandle<?>>();
					var resultRpc = (Rpc<?, ?>)Protocol.decode(
							localService::findProtocolFactoryHandle,
							ByteBuffer.Wrap(proxyRpc.Result.getData()),
							outFh);
					if (null != resultRpc) {
						if (null != rpc.getResponseHandle()) {
							@SuppressWarnings("rawtypes")
							var originHandle = (ProtocolHandle)rpc.getResponseHandle();
							localService.dispatchRpcResponse(resultRpc, originHandle, Objects.requireNonNull(outFh.value));
						} else if (rpc.getFuture() != null) {
							// 无 responseHandle 时按 future 路径回填 ResultCode 与 Result。
							rpc.setResultCode(resultRpc.getResultCode());
							//noinspection rawtypes,UnnecessaryLocalVariable
							Rpc rawRpc = rpc, rawResultRpc = resultRpc;
							rawRpc.Result = rawResultRpc.Result;
							rpc.getFuture().setRawResult(rpc);
						}
						// 既无 responseHandle 也无 future 时调用方忽略应答（如 trySendLeaderIs）。
					} else {
						logger.error("Server ProxyRequest({}) resultRpc decode not found.", proxyArgument.getRaftName());
					}
					// todo error handle
				} else {
					// todo error handle
					logger.error("Server ProxyRequest error={}", IModule.getErrorCode(proxyRpc.getResultCode()));
				}
				return 0;
			}, proxyServer.rpcTimeout);
		}

		// 未启用代理时直接发送（忽略应答）。
		return rpc.Send(sender);
	}
}
