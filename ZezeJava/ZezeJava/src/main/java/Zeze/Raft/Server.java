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
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 同时配置 Acceptor 和 Connector。
 * 逻辑上主要使用 Connector。
 * 两个Raft之间会有两个连接。
 * 【注意】
 * 为了简化配置，应用可以注册协议到Server，使用同一个Acceptor进行连接。
 */
public class Server extends HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(Server.class);

	private final Raft raft;
	private final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();
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

		private InstallSnapshotState installSnapshotState;

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

		InstallSnapshotState getInstallSnapshotState() {
			return installSnapshotState;
		}

		void setInstallSnapshotState(InstallSnapshotState value) {
			installSnapshotState = value;
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
		public void OnSocketClose(AsyncSocket closed, Throwable e) throws Exception {
			Raft raft = ((Server)closed.getService()).getRaft();
			Raft.executeImportantTask(() -> {
				// avoid deadlock: lock(socket), lock (Raft).
				raft.lock();
				try {
					if (getSocket() == closed) // check is owner
						raft.getLogSequence().endInstallSnapshot(this);
				} catch (Throwable ex) { // thread runner. logger.error
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
			Raft raft = ((Server)getService()).getRaft();
			Raft.executeImportantTask(() -> Task.call(() -> {
				raft.lock();
				try {
					raft.getLogSequence().trySendAppendEntries(this, null);
				} finally {
					raft.unlock();
				}
			}, "Start TrySendAppendEntries"));
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
				|| typeId == RequestVote.TypeId_
				|| typeId == AppendEntries.TypeId_
				|| typeId == InstallSnapshot.TypeId_
				|| typeId == LeaderIs.TypeId_;
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(P p, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			try {
				Raft.executeImportantTask(() -> Task.call(() -> responseHandle.handle(p), p));
			} catch (RejectedExecutionException e) {
				logger.debug("RejectedExecutionException for {}", p);
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
		return Task.call(() -> {
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
		}, p, Protocol::trySendResultCode);
	}

	/**
	 * Raft.Server的线程派发模式总是完全
	 */
	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so)
			throws Exception {
		// 不支持事务
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (isImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			// 内部协议总是使用明确返回值或者超时，不使用框架的错误时自动发送结果。
			Raft.executeImportantTask(() ->
					Task.call(() -> p.handle(this, factoryHandle), p, null));
			return;
		}

		var pTypeId = p.getTypeId();
		if (pTypeId == GetLeader.TypeId_ || pTypeId == StartServerConnector.TypeId_ || pTypeId == StopServerConnector.TypeId_) {
			// 这几条协议定义成了普通的用户请求，
			// 但是这条协议不需要自己是Leader也能工作，
			// 所以提前拦截，派发处理。see Raft::processGetLeader
			dispatchRaftRequest(p, () -> processRequest(p, factoryHandle),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		// User Request
		if (raft.isWorkingLeader()) {
			var raftRpc = (IRaftRpc)p;
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
		taskOneByOne.Execute(((IRaftRpc)p).getUnique(), func, name, cancel, mode);
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
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		// 没有判断是否和其他Raft-Node的连接。
		Task.executeUnsafe(() -> {
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
		}, "Raft.LeaderIs.Me", DispatchMode.Normal);
	}
}
