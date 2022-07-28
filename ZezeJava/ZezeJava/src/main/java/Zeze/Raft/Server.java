package Zeze.Raft;

import java.util.concurrent.RejectedExecutionException;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Services.HandshakeBoth;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
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
public final class Server extends HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(Server.class);

	private final Raft Raft;

	public Raft getRaft() {
		return Raft;
	}

	// 多个Raft实例才需要自定义配置名字，否则使用默认名字就可以了。
	public Server(Raft raft, String name, Zeze.Config config) throws Throwable {
		super(name, config);
		Raft = raft;
	}

	public static class ConnectorEx extends Connector {
		public ConnectorEx(String host, int port) {
			super(host, port);
			setMaxReconnectDelay(1000);
		}

		////////////////////////////////////////////////
		// Volatile state on leaders: (Reinitialized after election)
		// for each server, index of the next log entry to send to that server(initialized to leader last log index + 1)
		private long NextIndex;

		// for each server, index of highest log entry known to be replicated on server (initialized to 0, increases monotonically)
		private long MatchIndex;

		// 每个连接只允许存在一个AppendEntries。
		private AppendEntries Pending;

		private InstallSnapshotState InstallSnapshotState;

		private long AppendLogActiveTime = System.currentTimeMillis();

		long getNextIndex() {
			return NextIndex;
		}

		void setNextIndex(long value) {
			NextIndex = value;
		}

		long getMatchIndex() {
			return MatchIndex;
		}

		void setMatchIndex(long value) {
			MatchIndex = value;
		}

		AppendEntries getPending() {
			return Pending;
		}

		void setPending(AppendEntries value) {
			Pending = value;
		}

		InstallSnapshotState getInstallSnapshotState() {
			return InstallSnapshotState;
		}

		void setInstallSnapshotState(InstallSnapshotState value) {
			InstallSnapshotState = value;
		}

		long getAppendLogActiveTime() {
			return AppendLogActiveTime;
		}

		void setAppendLogActiveTime(long value) {
			AppendLogActiveTime = value;
		}

		@Override
		public void OnSocketClose(AsyncSocket closed, Throwable e) throws Throwable {
			Raft raft = ((Server)closed.getService()).getRaft();
			raft.getImportantThreadPool().execute(() -> {
				// avoid deadlock: lock(socket), lock (Raft).
				raft.lock();
				try {
					if (getSocket() == closed) { // check is owner
						try {
							raft.getLogSequence().EndInstallSnapshot(this);
						} catch (Throwable ex) {
							logger.error("Server.ConnectorEx.OnSocketClose", ex);
						}
					}
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
			raft.getImportantThreadPool().execute(() -> Task.Call(() -> {
				raft.lock();
				try {
					raft.getLogSequence().TrySendAppendEntries(this, null);
				} finally {
					raft.unlock();
				}
			}, "Start TrySendAppendEntries"));
		}
	}

	public static void CreateConnector(Service service, RaftConfig raftConf) {
		for (var node : raftConf.getNodes().values()) {
			if (raftConf.getName().equals(node.getName()))
				continue; // skip self.
			service.getConfig().AddConnector(new ConnectorEx(node.getHost(), node.getPort()));
		}
	}

	public static void CreateAcceptor(Service service, RaftConfig raftConf) {
		var node = raftConf.getNodes().get(raftConf.getName());
		if (node == null)
			throw new IllegalStateException("Raft.Name=" + raftConf.getName() + " Not In Node");
		service.getConfig().AddAcceptor(new Acceptor(node.getPort(), node.getHost()));
	}

	private boolean IsImportantProtocol(long typeId) {
		return IsHandshakeProtocol(typeId) // 【注意】下面这些模块的Id总是为0。
				|| typeId == RequestVote.TypeId_
				|| typeId == AppendEntries.TypeId_
				|| typeId == InstallSnapshot.TypeId_
				|| typeId == LeaderIs.TypeId_;
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(P p, ProtocolHandle<P> responseHandle,
														 ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
		if (IsImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			try {
				Raft.getImportantThreadPool().execute(() -> Task.Call(() -> responseHandle.handle(p), p));
			} catch (RejectedExecutionException e) {
				logger.debug("RejectedExecutionException for {}", p);
			}
			return;
		}
		super.DispatchRpcResponse(p, responseHandle, factoryHandle);
	}

	private final TaskOneByOneByKey TaskOneByOne = new TaskOneByOneByKey();

	@SuppressWarnings("UnusedReturnValue")
	private <P extends Protocol<?>> long ProcessRequest(P p, ProtocolFactoryHandle<P> factoryHandle) {
		return Task.Call(() -> {
			if (Raft.WaitLeaderReady()) {
				UniqueRequestState state = Raft.getLogSequence().TryGetRequestState(p);
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
					return factoryHandle.Handle.handle(p);
				}
				p.SendResultCode(Procedure.RaftExpired);
				return 0L;
			}
			TrySendLeaderIs(p.getSender());
			return 0L;
		}, p, Protocol::trySendResultCode);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		if (IsImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			// 内部协议总是使用明确返回值或者超时，不使用框架的错误时自动发送结果。
			Raft.getImportantThreadPool().execute(() ->
					Task.Call(() -> factoryHandle.Handle.handle(p), p, null));
			return;
		}
		// User Request
		if (Raft.isWorkingLeader()) {
			var raftRpc = (IRaftRpc)p;
			if (raftRpc.getUnique().getRequestId() <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return;
			}
			//【防止重复的请求】
			// see Log.java::LogSequence.TryApply
			TaskOneByOne.Execute(raftRpc.getUnique(), () -> ProcessRequest(p, factoryHandle),
					p.getClass().getName(), () -> p.SendResultCode(Procedure.RaftRetry), factoryHandle.Mode);
			return;
		}

		TrySendLeaderIs(p.getSender());

		// 选举中
		// DO NOT process application request.
	}

	private void TrySendLeaderIs(AsyncSocket sender) {
		String leaderId = Raft.getLeaderId();
		if (leaderId == null || leaderId.isEmpty())
			return;
		if (Raft.getName().equals(leaderId) && !Raft.isLeader())
			return;
		// redirect
		var redirect = new LeaderIs();
		redirect.Argument.setTerm(Raft.getLogSequence().getTerm());
		redirect.Argument.setLeaderId(leaderId); // maybe empty
		redirect.Argument.setLeader(Raft.isLeader());
		redirect.Send(sender); // ignore response
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Throwable {
		super.OnHandshakeDone(so);

		// 没有判断是否和其他Raft-Node的连接。
		Task.run(() -> {
			Raft.lock();
			try {
				if (Raft.isReadyLeader()) {
					var r = new LeaderIs();
					r.Argument.setTerm(Raft.getLogSequence().getTerm());
					r.Argument.setLeaderId(Raft.getLeaderId());
					r.Argument.setLeader(Raft.isLeader());
					r.Send(so); // skip result
				}
			} finally {
				Raft.unlock();
			}
		}, "Raft.LeaderIs.Me", DispatchMode.Normal);
	}
}
