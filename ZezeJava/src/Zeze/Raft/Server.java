package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

/** 
 同时配置 Acceptor 和 Connector。
 逻辑上主要使用 Connector。
 两个Raft之间会有两个连接。
 【注意】
 为了简化配置，应用可以注册协议到Server，使用同一个Acceptor进行连接。
*/
public final class Server extends Services.HandshakeBoth {
	private Raft Raft;
	public Raft getRaft() {
		return Raft;
	}

	// 多个Raft实例才需要自定义配置名字，否则使用默认名字就可以了。
	public Server(Raft raft, String name, Config config) {
		super(name, config);
		Raft = raft;
	}

	public static class ConnectorEx extends Connector {
		public ConnectorEx(String host, int port) {
			super(host, port);

		}

		////////////////////////////////////////////////
		// Volatile state on leaders:
		// (Reinitialized after election)
		/** 
		 for each server, index of the next log entry
		 to send to that server(initialized to leader
		 last log index + 1)
		*/
		private long NextIndex;
		public final long getNextIndex() {
			return NextIndex;
		}
		public final void setNextIndex(long value) {
			NextIndex = value;
		}

		/** 
		 for each server, index of highest log entry
		 known to be replicated on server
		 (initialized to 0, increases monotonically)
		*/
		private long MatchIndex;
		public final long getMatchIndex() {
			return MatchIndex;
		}
		public final void setMatchIndex(long value) {
			MatchIndex = value;
		}

		/** 
		 正在安装Snapshot，用来阻止新的安装。
		*/
		private boolean InstallSnapshotting;
		public final boolean getInstallSnapshotting() {
			return InstallSnapshotting;
		}
		public final void setInstallSnapshotting(boolean value) {
			InstallSnapshotting = value;
		}

		/** 
		 每个连接只允许存在一个AppendEntries。
		*/
		private AppendEntries Pending;
		public final AppendEntries getPending() {
			return Pending;
		}
		public final void setPending(AppendEntries value) {
			Pending = value;
		}

		@Override
		public void OnSocketClose(AsyncSocket closed) {
			Zeze.Gen.Service tempVar = closed.getService();
			var server = tempVar instanceof Server ? (Server)tempVar : null;
			synchronized (server.getRaft()) {
				// 安装快照没有续传能力，网络断开以后，重置状态。
				// 以后需要的时候，再次启动新的安装流程。
				setInstallSnapshotting(false);
				server.getRaft().getLogSequence().getInstallSnapshotting().remove(getName());
			}
			super.OnSocketClose(closed);
		}
	}

	public static void CreateConnector(Service service, RaftConfig raftconf) {
		for (var node : raftconf.getNodes().values()) {
			if (raftconf.getName().equals(node.Name)) {
				continue; // skip self.
			}
			service.Config.AddConnector(new ConnectorEx(node.Host, node.Port));
		}
	}

	public static void CreateAcceptor(Service service, RaftConfig raftconf) {
		TValue node;
		tangible.OutObject<TValue> tempOut_node = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (false == raftconf.getNodes().TryGetValue(raftconf.getName(), tempOut_node)) {
		node = tempOut_node.outArgValue;
			throw new RuntimeException("raft Name Not In Node");
		}
	else {
		node = tempOut_node.outArgValue;
	}
		service.Config.AddAcceptor(new Acceptor(node.Port, node.Host));
	}

	private boolean IsImportantProtocol(int typeId) {
		return IsHandshakeProtocol(typeId) || typeId == RequestVote.ProtocolId_ || typeId == AppendEntries.ProtocolId_ || typeId == InstallSnapshot.ProtocolId_ || typeId == LeaderIs.ProtocolId_;
	}

	@Override
	public void DispatchRpcResponse(Protocol p, tangible.Func1Param<Protocol, Integer> responseHandle, ProtocolFactoryHandle factoryHandle) {
		if (IsImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			getRaft().getImportantThreadPool().QueueUserWorkItem(() -> Util.Task.Call(() -> responseHandle.invoke(p), p));
			return;
		}

		super.DispatchRpcResponse(p, responseHandle, factoryHandle);
	}

	private Util.TaskOneByOneByKey TaskOneByOne = new Util.TaskOneByOneByKey();
	public Util.TaskOneByOneByKey getTaskOneByOne() {
		return TaskOneByOne;
	}

	private int ProcessRequest(Protocol p, ProtocolFactoryHandle factoryHandle) {
		return Util.Task.Call(() -> {
				if (getRaft().WaitLeaderReady()) {
					TValue max;
					tangible.OutObject<TValue> tempOut_max = new tangible.OutObject<TValue>();
					if (getRaft().getLogSequence().getLastAppliedAppRpcUniqueRequestId().TryGetValue(p.Sender.RemoteAddress, tempOut_max)) {
						max = tempOut_max.outArgValue;
						if (p.UniqueRequestId <= max) {
							p.SendResultCode(Procedure.DuplicateRequest);
							return Procedure.DuplicateRequest;
						}
					}
					else {
						max = tempOut_max.outArgValue;
					}
					return factoryHandle.Handle(p);
				}
				TrySendLeaderIs(p.Sender);
				return Procedure.LogicError;
		}, p, (p, code) -> p.SendResultCode(code));
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// 防止Client不进入加密，直接发送用户协议。
		if (false == IsHandshakeProtocol(p.getTypeId())) {
			p.Sender.VerifySecurity();
		}

		if (IsImportantProtocol(p.getTypeId())) {
			// 不能在默认线程中执行，使用专用线程池，保证这些协议得到处理。
			// 内部协议总是使用明确返回值或者超时，不使用框架的错误时自动发送结果。
			getRaft().getImportantThreadPool().QueueUserWorkItem(() -> Util.Task.Call(() -> factoryHandle.Handle(p), p, null));
			return;
		}
		// User Request

		if (getRaft().isLeader()) {
			if (p.UniqueRequestId <= 0) {
				p.SendResultCode(Procedure.ErrorRequestId);
				return;
			}

			// 默认0，每个远程ip地址允许并发。
			// 不直接包含port信息，client.port容易改变。

			//【防止重复的请求】
			// see Log.cs::LogSequence.TryApply
			getTaskOneByOne().Execute(p.Sender.RemoteAddress, () -> ProcessRequest(p, factoryHandle), p.getClass().getName(), () -> p.SendResultCode(Procedure.CancelExcption));
			return;
		}

		TrySendLeaderIs(p.Sender);

		// 选举中
		// DONOT process application request.
	}

	private void TrySendLeaderIs(AsyncSocket sender) {
		if (getRaft().getHasLeader()) {
			// redirect
			var redirect = new LeaderIs();
			redirect.getArgument().setTerm(getRaft().getLogSequence().getTerm());
			redirect.getArgument().setLeaderId(getRaft().getLeaderId());
			redirect.Send(sender); // ignore response
			// DONOT process application request.
			return;
		}
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) {
		super.OnHandshakeDone(so);

		// 没有判断是否和其他Raft-Node的连接。
		if (getRaft().isLeader() && getRaft().getLeaderReadyEvent().WaitOne(0)) {
			var r = new LeaderIs();
			r.getArgument().setTerm(getRaft().getLogSequence().getTerm());
			r.getArgument().setLeaderId(getRaft().getLeaderId());
			r.Send(so); // skip result
		}
	}
}