package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

	public Agent(String name, Application zeze, RaftConfig raftconf) {
		this(name, zeze, raftconf, null);
	}

	public Agent(String name, Application zeze) {
		this(name, zeze, null, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Agent(string name, Application zeze, RaftConfig raftconf = null, Action<Agent, Action> onLeaderChanged = null)
	public Agent(String name, Application zeze, RaftConfig raftconf, tangible.Action2Param<Agent, tangible.Action0Param> onLeaderChanged) {
		Init(new NetClient(this, name, zeze), raftconf, onLeaderChanged);
	}

	/** 
	 
	 
	 @param raftconf
	 @param config
	 @param onLeaderChanged
	 @param name
	*/

	public Agent(String name, RaftConfig raftconf, Zeze.Config config) {
		this(name, raftconf, config, null);
	}

	public Agent(String name, RaftConfig raftconf) {
		this(name, raftconf, null, null);
	}

	public Agent(String name) {
		this(name, null, null, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Agent(string name, RaftConfig raftconf = null, Zeze.Config config = null, Action<Agent, Action> onLeaderChanged = null)
	public Agent(String name, RaftConfig raftconf, Config config, tangible.Action2Param<Agent, tangible.Action0Param> onLeaderChanged) {
		if (null == config) {
			config = Config.Load(null);
		}

		Init(new NetClient(this, name, config), raftconf, onLeaderChanged);
	}

	private void Init(NetClient client, RaftConfig raftconf, tangible.Action2Param<Agent, tangible.Action0Param> onLeaderChanged) {
		setOnLeaderChanged(::onLeaderChanged);

		if (null == raftconf) {
			raftconf = RaftConfig.Load();
		}

		setRaftConfig(raftconf);
		setClient(client);

		if (getClient().getConfig().AcceptorCount() != 0) {
			throw new RuntimeException("Acceptor Found!");
		}

		if (getClient().getConfig().ConnectorCount() != 0) {
			throw new RuntimeException("Connector Found!");
		}

		for (var node : getRaftConfig().getNodes().values()) {
			getClient().getConfig().AddConnector(new ConnectorEx(node.Host, node.Port));
		}

		getClient().AddFactoryHandle((new LeaderIs()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new LeaderIs(), Handle = ProcessLeaderIs});
	}

	private int ProcessLeaderIs(Protocol p) {
		var r = p instanceof LeaderIs ? (LeaderIs)p : null;
		logger.Debug("{0}: {1}", getName(), r);

		var node = getClient().getConfig().FindConnector(r.getArgument().getLeaderId());
		if (null == node) {
			// 当前 Agent 没有 Leader 的配置，创建一个。
			// 由于 Agent 在新增 node 时也会得到新配置广播，
			// 一般不会发生这种情况。
			var address = r.getArgument().getLeaderId().split("[:]", -1);
			tangible.OutObject<var> tempOut_node = new tangible.OutObject<var>();
			if (getClient().getConfig().TryGetOrAddConnector(address[0], Integer.parseInt(address[1]), true, tempOut_node)) {
			node = tempOut_node.outArgValue;
				node.Start();
			}
		else {
			node = tempOut_node.outArgValue;
		}
		}

		if (TrySetLeader(r, node instanceof ConnectorEx ? (ConnectorEx)node : null)) {
			if (r.getSender().getConnector().getName().equals(r.getArgument().getLeaderId())) {
				// 来自 Leader 的公告。
				if (null != getOnLeaderChanged()) {
					OnLeaderChanged(this, SetReady);
				}
				else {
					SetReady();
				}
			}
			else {
				// 从 Follower 得到的重定向，原则上不需要处理。
				// 等待 LeaderIs 的通告即可。但是为了防止LeaderIs丢失，就处理一下吧。
				// 【实际上和上面的处理逻辑一样】。
				// 此时Leader可能没有准备好，但是提前给Leader发送请求是可以的。
				if (null != getOnLeaderChanged()) {
					OnLeaderChanged(this, SetReady);
				}
				else {
					SetReady();
				}
			}

		}
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private void SetReady() {
		// ReSendPendingRpc
		for (var rpc : getPending().values()) {
			if (getNotAutoResend().containsKey(rpc)) {
				continue;
			}

			if (rpc.Send(_Leader == null ? null : _Leader.getSocket())) {
				// 这里发送失败，等待新的 LeaderIs 通告再继续。
				V _;
				tangible.OutObject<V> tempOut__ = new tangible.OutObject<V>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				getPending().TryRemove(rpc, tempOut__);
			_ = tempOut__.outArgValue;
			}
		}
	}

	private void CollectPendingRpc(ConnectorEx oldLeader, AsyncSocket oldSocket) {
		if (null != oldLeader) {
			// 再 Rpc.UserState 里面记录发送目的的ConnectorEx，然后这里严格判断？
			// 由于一个时候只有Leader，所以直接使用Sender也足够了吧。
			var ctxSends = getClient().GetRpcContextsToSender(oldSocket);
			var ctxPending = getClient().RemoveRpcContets(ctxSends.keySet());
			for (var rpc : ctxPending) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				getPending().TryAdd(rpc, rpc);
			}
		}
	}

	public boolean TrySetLeader(LeaderIs r, ConnectorEx newLeader) {
		synchronized (this) {
			if (r.getArgument().getTerm() < newLeader.getTerm()) {
				logger.Warn("{0} Skip LeaderIs {1}", getName(), r);
				return false;
			}
			if (_Leader != newLeader) {
				// 把旧的_Leader的没有返回结果的请求收集起来，准备重新发送。
				CollectPendingRpc(_Leader, _Leader == null ? null : _Leader.getSocket());
			}
			newLeader.setTerm(r.getArgument().getTerm());
			_Leader = newLeader;
			return true;
		}
	}

	public boolean TryClearLeader(ConnectorEx oldLeader, AsyncSocket oldSocket) {
		synchronized (this) {
			if (_Leader == oldLeader) {
				CollectPendingRpc(_Leader, oldSocket);
				_Leader = null;
				return true;
			}
			return false;
		}
	}

	public final static class NetClient extends Services.HandshakeClient {
		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private Agent Agent;
		public Agent getAgent() {
			return Agent;
		}

		public NetClient(Agent agent, String name, Application zeze) {
			super(name, zeze);
			Agent = agent;
		}

		public NetClient(Agent agent, String name, Config config) {
			super(name, config);
			Agent = agent;
		}

		@Override
		public void OnSocketDisposed(AsyncSocket so) {
			Zeze.Net.Connector tempVar = so.getConnector();
			var connector = tempVar instanceof ConnectorEx ? (ConnectorEx)tempVar : null;
			getAgent().TryClearLeader(connector, so);
			super.OnSocketDisposed(so);
			connector.setAutoReconnect(true);
			connector.TryReconnect();
		}
	}