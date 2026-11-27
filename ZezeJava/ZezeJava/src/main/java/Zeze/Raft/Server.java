package Zeze.Raft;

import java.util.concurrent.RejectedExecutionException;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeBoth;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Raft 节点通信服务：同时承载节点间 Acceptor/Connector 与应用接入。
 * 节点间逻辑上主要使用 Connector；注册的应用协议须为 IRaftRpc 族，以支持复制与唯一请求去重。
 */
public class Server extends HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(Server.class);

	private final Raft raft;
	protected final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();
	private volatile @Nullable ProxyServer proxyServer;

	public @Nullable ProxyServer getProxyServer() {
		return proxyServer;
	}

	public void setProxyServer(@Nullable ProxyServer proxyServer) {
		lock();
		try {
			this.proxyServer = proxyServer;
		} finally {
			unlock();
		}
	}

	public Raft getRaft() {
		return raft;
	}

	// 仅多 Raft 实例部署时需要自定义配置名。
	public Server(Raft raft, String name, Config config) {
		super(name, config);
		this.raft = raft;
		setNoProcedure(true);
	}

	public static class ConnectorEx extends Connector {
		// Leader 易失状态（选举后重置）：待发送给该节点的下一条日志索引
		private long nextIndex;

		// Leader 易失状态：已知已复制到该节点的最高日志索引（单调递增）
		private long matchIndex;

		// 每个连接同时仅允许一个在途 AppendEntries。
		private @Nullable AppendEntries pending;

		private long appendLogActiveTime = System.currentTimeMillis();
		private long heartbeatTime = System.currentTimeMillis();

		public ConnectorEx(String host, int port) {
			super(host, port);
			setMaxReconnectDelay(1000);
		}

		long getNextIndex() {
			return nextIndex;
		}

		void setNextIndex(long value) {
			nextIndex = value;
		}

		long getMatchIndex() {
			return matchIndex;
		}

		void setMatchIndex(long value) {
			matchIndex = value;
		}

		@Nullable AppendEntries getPending() {
			return pending;
		}

		void setPending(@Nullable AppendEntries value) {
			pending = value;
		}

		long getAppendLogActiveTime() {
			return appendLogActiveTime;
		}

		void setAppendLogActiveTime(long value) {
			appendLogActiveTime = value;
		}

		void setHeartbeatTime(long value) {
			heartbeatTime = value;
		}

		long getHeartbeatTime() {
			return heartbeatTime;
		}

		@Override
		public void OnSocketClose(AsyncSocket closed, @Nullable Throwable e) throws Exception {
			//noinspection DataFlowIssue
			Raft raft = (((Server)closed.getService())).getRaft();
			Raft.executeImportantTask(() -> {
				// 异步切线程避免 lock(socket) -> lock(Raft) 死锁。
				raft.lock();
				try {
					if (!raft.isShutdown)
						raft.getLogSequence().getSendSnapshotting().endIfSocket(this, closed);
				} catch (Throwable ex) {
					logger.error("Server.ConnectorEx.OnSocketClose", ex);
				} finally {
					raft.unlock();
				}
			});
			super.OnSocketClose(closed, e);
		}

		@Override
		public void OnSocketHandshakeDone(AsyncSocket so) {
			super.OnSocketHandshakeDone(so);
			//noinspection DataFlowIssue
			Raft raft = (((Server)getService())).getRaft();
			Raft.executeImportantTask(() -> TaskSpec.ofAction(() -> {
				raft.lock();
				try {
					raft.getLogSequence().trySendAppendEntries(this, null);
				} finally {
					raft.unlock();
				}
			}).name("Start TrySendAppendEntries").call());
		}
	}

	public static void createConnector(Service service, RaftConfig raftConf) {
		for (var node : raftConf.getNodes().values()) {
			//noinspection DataFlowIssue
			if (raftConf.getName().equals(node.getName()))
				continue; // 跳过自身
			service.getConfig().addConnector(new ConnectorEx(node.getHost(), node.getPort()));
		}
	}

	public static void createAcceptor(Service service, RaftConfig raftConf) {
		var node = raftConf.getNodes().get(raftConf.getName());
		if (node == null)
			throw new IllegalStateException("Raft.Name=" + raftConf.getName() + " Not In Node");
		service.getConfig().addAcceptor(new Acceptor(node.getPort(), node.getHost()));
	}

	private boolean isImportantProtocol(long typeId) {
		return isHandshakeProtocol(typeId) // 内部核心协议（模块 ID 均为 0）
				|| typeId == PreVote.TypeId_
				|| typeId == RequestVote.TypeId_
				|| typeId == AppendEntries.TypeId_
				|| typeId == InstallSnapshot.TypeId_
				|| typeId == LeaderIs.TypeId_;
	}

	// 注册期校验：应用协议必须实现 IRaftRpc，仅对内部 Raft/握手/代理协议放行。
	@Override
	public void AddFactoryHandle(long type, ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (!IRaftRpc.class.isAssignableFrom(factory.Class)
				&& !isImportantProtocol(type) && !isHandshakeProtocol(type) && type != ProxyRequest.TypeId_)
			throw new IllegalArgumentException("Raft.Server only accepts IRaftRpc application protocols"
					+ " (raft replication requires UniqueRequestId); internal raft/handshake protocols"
					+ " are whitelisted: " + factory.Class.getName());
		super.AddFactoryHandle(type, factory);
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(P p, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 内部重要协议走专用线程池，避免被业务任务阻塞。
			try {
				Raft.executeImportantTask(() -> TaskSpec.ofFunc(() -> responseHandle.handle(p), p).call());
			} catch (RejectedExecutionException e) {
				logger.warn("RejectedExecutionException for {}", p);
			}
			return;
		}
		dispatchRaftRpcResponse(p, responseHandle, factoryHandle);
	}

	public <P extends Protocol<?>> void dispatchRaftRpcResponse(P p, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		super.dispatchRpcResponse(p, responseHandle, factoryHandle);
	}

	public long processRequest(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) {
		return TaskSpec.ofFunc(() -> {
			if (raft.waitLeaderReady()) {
				UniqueRequestState state = raft.getLogSequence().tryGetRequestState(p);
				if (state != null) {
					if (state != UniqueRequestState.NOT_FOUND) {
						if (state.isApplied()) {
							//noinspection DataFlowIssue
							p.SendResultCode(Procedure.RaftApplied,
									state.getRpcResult().size() > 0 ? state.getRpcResult() : null);
							return 0L;
						}
						p.SendResultCode(Procedure.DuplicateRequest);
						return 0L;
					}
					return p.handle(this, factoryHandle);
				}
				p.SendResultCode(Procedure.RaftExpired);
				return 0L;
			}
			trySendLeaderIs(p.getSender());
			return 0L;
		}, p, (__, code) -> p.trySendResultCode(code)).call();
	}

	/**
	 * Raft.Server 的派发模式：总是完全派发（不使用事务）。
	 */
	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so)
			throws Exception {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 内部协议走专用线程池，依赖明确返回值或超时，不使用框架自动回错。
			Raft.executeImportantTask(() ->
					TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p).call());
			return;
		}

		var pTypeId = p.getTypeId();
		if (pTypeId == GetLeader.TypeId_ || pTypeId == StartServerConnector.TypeId_ || pTypeId == StopServerConnector.TypeId_) {
			// 控制类请求无需 Leader 身份，且 Agent 直发不填 createTime，提前拦截派发以跳过 Leader 门槛与去重校验。
			dispatchRaftRequest(p, () -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle),
							p, (__, code) -> p.trySendResultCode(code)).call(),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		if (!(p instanceof IRaftRpc raftRpc)) {
			// 非 IRaftRpc 流量按普通协议派发，不参与 Raft 复制，避免强转异常中断连接。
			logger.warn("Raft.Server dispatch non-IRaftRpc protocol as plain: {}", p);
			super.dispatchProtocol(p, factoryHandle);
			return;
		}
		if (raft.isWorkingLeader()) {
			if (raftRpc.getUnique().getRequestId() <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return;
			}
			// 经 processRequest 执行唯一请求去重
			dispatchRaftRequest(p, () -> processRequest(p, factoryHandle),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		// 非 Leader 或选举中：尝试重定向，不处理应用请求。
		trySendLeaderIs(p.getSender());
	}

	@SuppressWarnings("RedundantThrows")
	public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel, DispatchMode mode)
			throws Exception {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode)
				.executeOneByOne(((IRaftRpc)p).getUnique(), taskOneByOne);
	}

	public void trySendLeaderIs(@Nullable AsyncSocket sender) {
		String leaderId = raft.getLeaderId();
		if (leaderId == null || leaderId.isEmpty())
			return;
		if (raft.getName().equals(leaderId) && !raft.isLeader())
			return;
		var redirect = new LeaderIs();
		redirect.Argument.setTerm(raft.getLogSequence().getTerm());
		redirect.Argument.setLeaderId(leaderId);
		redirect.Argument.setLeader(raft.isLeader());
		ProxyServer.send(this, proxyServer, redirect, raft.getName(), sender);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		// 连接握手完成时若自身为就绪 Leader 则主动通告（不区分是否节点间连接）。
		TaskSpec.ofAction(() -> {
			raft.lock();
			try {
				if (raft.isReadyLeader()) {
					var r = new LeaderIs();
					r.Argument.setTerm(raft.getLogSequence().getTerm());
					r.Argument.setLeaderId(raft.getLeaderId());
					r.Argument.setLeader(raft.isLeader());
					r.Send(so);
				}
			} finally {
				raft.unlock();
			}
		}).name("Raft.LeaderIs.Me").runNow();
	}
}
