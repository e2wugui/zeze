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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 同时配置 Acceptor 和 Connector。
 * 逻辑上主要使用 Connector。
 * 两个Raft之间会有两个连接。
 * 【注意】
 * 为了简化配置，应用可以注册协议到Server，使用同一个Acceptor进行连接。
 * 【注意】注册的应用协议必须是 RaftRpc/IRaftRpc 族（raft复制与唯一请求去重依赖
 * UniqueRequestId，非IRaftRpc注册在启动期被拒绝）；到达的非IRaftRpc流量按普通
 * 协议派发（不参与raft复制，不中断连接）。
 */
public class Server extends HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(Server.class);

	private final Raft raft;
	protected final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();
	private volatile ProxyServer proxyServer;

	public ProxyServer getProxyServer() {
		return proxyServer;
	}

	public void setProxyServer(ProxyServer proxyServer) {
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

	// 多个Raft实例才需要自定义配置名字，否则使用默认名字就可以了。
	public Server(Raft raft, String name, Config config) {
		super(name, config);
		this.raft = raft;
		setNoProcedure(true);
	}

	public static class ConnectorEx extends Connector {
		// Volatile state on leaders: (Reinitialized after election)
		// for each server, index of the next log entry to send to that server(initialized to leader last log index + 1)
		private long nextIndex;

		// for each server, index of highest log entry known to be replicated on server (initialized to 0, increases monotonically)
		private long matchIndex;

		// 每个连接只允许存在一个AppendEntries。
		private AppendEntries pending;

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

		AppendEntries getPending() {
			return pending;
		}

		void setPending(AppendEntries value) {
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
		public void OnSocketClose(@NotNull AsyncSocket closed, @Nullable Throwable e) throws Exception {
			Raft raft = ((Server)closed.getService()).getRaft();
			Raft.executeImportantTask(() -> {
				// avoid deadlock: lock(socket), lock (Raft).
				raft.lock();
				try {
					if (getSocket() == closed) // check is owner
						raft.getLogSequence().getSendSnapshotting().end(this);
				} catch (Throwable ex) { // thread runner. logger.error
					logger.error("Server.ConnectorEx.OnSocketClose", ex);
				} finally {
					raft.unlock();
				}
			});
			super.OnSocketClose(closed, e);
		}

		@Override
		public void OnSocketHandshakeDone(@NotNull AsyncSocket so) {
			super.OnSocketHandshakeDone(so);
			Raft raft = ((Server)getService()).getRaft();
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
			if (raftConf.getName().equals(node.getName()))
				continue; // skip self.
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
		return isHandshakeProtocol(typeId) // 【注意】下面这些模块的Id总是为0。
				|| typeId == PreVote.TypeId_
				|| typeId == RequestVote.TypeId_
				|| typeId == AppendEntries.TypeId_
				|| typeId == InstallSnapshot.TypeId_
				|| typeId == LeaderIs.TypeId_;
	}

	// 【FND8-41】注册期守卫：Raft复制语义（UniqueRequestId去重/RaftApplied回放）要求
	// 应用协议为IRaftRpc族；内部Raft协议与握手协议（自身不是IRaftRpc）白名单放行，
	// 误注册拦在启动期，而非Leader态强转杀连接。
	@Override
	public void AddFactoryHandle(long type, @NotNull ProtocolFactoryHandle<? extends Protocol<?>> factory) {
		if (!IRaftRpc.class.isAssignableFrom(factory.Class)
				&& !isImportantProtocol(type) && !isHandshakeProtocol(type) && type != ProxyRequest.TypeId_)
			throw new IllegalArgumentException("Raft.Server only accepts IRaftRpc application protocols"
					+ " (raft replication requires UniqueRequestId); internal raft/handshake protocols"
					+ " are whitelisted: " + factory.Class.getName());
		super.AddFactoryHandle(type, factory);
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P p, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
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
		}, p, Protocol::trySendResultCode).call();
	}

	/**
	 * Raft.Server的线程派发模式总是完全
	 */
	@Override
	public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb, @NotNull ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so)
			throws Exception {
		// 不支持事务
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			// 内部协议总是使用明确返回值或者超时，不使用框架的错误时自动发送结果。
			Raft.executeImportantTask(() ->
					TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p).call());
			return;
		}

		var pTypeId = p.getTypeId();
		if (pTypeId == GetLeader.TypeId_ || pTypeId == StartServerConnector.TypeId_ || pTypeId == StopServerConnector.TypeId_) {
			// 这几条协议定义成了普通的用户请求，
			// 但是这条协议不需要自己是Leader也能工作，
			// 所以提前拦截，派发处理。see Raft::processGetLeader
			// 【R3-F2】直接派发p.handle：原来转processRequest会强制leader-ready门槛+
			// 唯一请求createTime校验，而Agent直发不填createTime（=0），
			// isUniqueRequestCreateTimeValid恒拒→RaftExpired，processGetLeader/processStartServer/
			// processStopServer在唯一发送路径上不可达；保留dispatchRaftRequest包装、错误码回发
			// 与RaftRetry错误路径（与原processRequest内的包装同构，仅去掉门槛与去重校验）。
			dispatchRaftRequest(p, () -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle),
							p, Protocol::trySendResultCode).call(),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		// User Request
		if (!(p instanceof IRaftRpc raftRpc)) {
			// 【FND8-41】非IRaftRpc协议（误注册/误发）：不裸强转——Leader态CCE从IO线程
			// 一路抛出杀掉整个连接（含其上全部Raft流量）。按普通协议经基类派发：
			// TaskSpec尊重注册的DispatchMode（不在IO线程跑handle），setNoProcedure(true)
			// 走非事务分支；处理失败时Rpc族由trySendResultCode回错误码（普通Protocol
			// 无应答通道，仅日志），连接不中断。
			logger.warn("Raft.Server dispatch non-IRaftRpc protocol as plain: {}", p);
			super.dispatchProtocol(p, factoryHandle);
			return;
		}
		if (raft.isWorkingLeader()) {
			if (raftRpc.getUnique().getRequestId() <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return;
			}
			//【防止重复的请求】
			// see Log.java::LogSequence.TryApply
			dispatchRaftRequest((RaftRpc<?, ?>)raftRpc, () -> processRequest(p, factoryHandle),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		trySendLeaderIs(p.getSender());

		// 选举中
		// DO NOT process application request.
	}

	@SuppressWarnings("RedundantThrows")
	public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel, DispatchMode mode)
			throws Exception {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode)
				.executeOneByOne(((IRaftRpc)p).getUnique(), taskOneByOne);
	}

	public void trySendLeaderIs(AsyncSocket sender) {
		String leaderId = raft.getLeaderId();
		if (leaderId == null || leaderId.isEmpty())
			return;
		if (raft.getName().equals(leaderId) && !raft.isLeader())
			return;
		// redirect
		var redirect = new LeaderIs();
		redirect.Argument.setTerm(raft.getLogSequence().getTerm());
		redirect.Argument.setLeaderId(leaderId); // maybe empty
		redirect.Argument.setLeader(raft.isLeader());
		ProxyServer.send(this, proxyServer, redirect, raft.getName(), sender);
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		// 没有判断是否和其他Raft-Node的连接。
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
