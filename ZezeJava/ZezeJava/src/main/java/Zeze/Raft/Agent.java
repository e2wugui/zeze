package Zeze.Raft;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToLongFunction;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Agent {
	private static final Logger logger = LogManager.getLogger(Agent.class);

	// 保证在Raft-Server检查UniqueRequestId唯一性过期前唯一即可。
	// 使用持久化是为了避免短时间重启，Id重复。
	private final PersistentAtomicLong UniqueRequestIdGenerator;
	private RaftConfig RaftConfig;
	private NetClient Client;
	private volatile ConnectorEx _Leader;
	private final LongConcurrentHashMap<RaftRpc<?, ?>> Pending = new LongConcurrentHashMap<>();
	private long Term;
	public boolean DispatchProtocolToInternalThreadPool;
	private int PendingLimit = 5000; // -1 no limit

	// 加急请求ReSend时优先发送，多个请求不保证顺序。这个应该仅用于Login之类的特殊协议，一般来说只有一个。
	private final LongConcurrentHashMap<RaftRpc<?, ?>> UrgentPending = new LongConcurrentHashMap<>();
	private Action1<Agent> OnSetLeader;
	private final Lock mutex = new ReentrantLock();

	public RaftConfig getRaftConfig() {
		return RaftConfig;
	}

	public int getPendingLimit() {
		return PendingLimit;
	}

	public void setPendingLimit(int value) {
		PendingLimit = value;
	}

	public NetClient getClient() {
		return Client;
	}

	public String getName() {
		return Client.getName();
	}

	public ConnectorEx getLeader() {
		return _Leader;
	}

	public long getTerm() {
		return Term;
	}

	public Action1<Agent> getOnSetLeader() {
		return OnSetLeader;
	}

	public void setOnSetLeader(Action1<Agent> onSetLeader) {
		OnSetLeader = onSetLeader;
	}

	public <TArgument extends Bean, TResult extends Bean> void Send(RaftRpc<TArgument, TResult> rpc,
																	ToLongFunction<Protocol<?>> handle) {
		Send(rpc, handle, false);
	}

	/**
	 * 发送Rpc请求。
	 */
	public <TArgument extends Bean, TResult extends Bean> void Send(RaftRpc<TArgument, TResult> rpc,
																	ToLongFunction<Protocol<?>> handle,
																	boolean urgent) {
		if (handle == null)
			throw new NullPointerException();
		if (PendingLimit > 0 && Pending.size() > PendingLimit) // UrgentPending不限制。
			throw new RuntimeException("too many pending");

		// 由于interface不能把setter弄成保护的，实际上外面可以修改。
		// 简单检查一下吧。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(UniqueRequestIdGenerator.next());
		// 外面可以设置clientId，默认使用Generator.getName();
		if (rpc.getUnique().getClientId().isEmpty())
			rpc.getUnique().setClientId(UniqueRequestIdGenerator.getName());
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0) // set default timeout
			rpc.setTimeout(RaftConfig.getAppendEntriesTimeout());

		rpc.setUrgent(urgent);
		var pending = urgent ? UrgentPending : Pending;
		rpc.Handle = handle;
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);

		rpc.setResponseHandle(p -> SendHandle(p, rpc));
		ConnectorEx leader = _Leader;
		if (!rpc.Send(leader != null ? leader.TryGetReadySocket() : null))
			logger.warn("Send failed: leader={}, rpc={}", leader, rpc);
	}

	private <TArgument extends Bean, TResult extends Bean> long SendHandle(Rpc<TArgument, TResult> p,
																		   RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || IsRetryError(net.getResultCode()))
			return Procedure.Success; // Pending Will Resend.

		long requestId = rpc.getUnique().getRequestId();
		if (Pending.remove(requestId) != null || UrgentPending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());
			rpc.setUserState(net.getUserState());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			logger.debug("Agent Rpc={} RequestId={} ResultCode={} Sender={}",
					rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			return rpc.Handle.applyAsLong(rpc);
		}
		return Procedure.Success;
	}

	private boolean IsRetryError(long error) {
		return error == Procedure.CancelException ||
				error == Procedure.RaftRetry ||
				error == Procedure.DuplicateRequest;
	}

	@SuppressWarnings("SameReturnValue")
	private <TArgument extends Bean, TResult extends Bean> long SendForWaitHandle(Rpc<TArgument, TResult> p,
																				  RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || IsRetryError(net.getResultCode()))
			return Procedure.Success; // Pending Will Resend.

		long requestId = rpc.getUnique().getRequestId();
		if (Pending.remove(requestId) != null || UrgentPending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());
			rpc.setUserState(net.getUserState());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			logger.debug("Agent Rpc={} RequestId={} ResultCode={} Sender={}",
					rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			rpc.Future.SetResult(rpc);
		}
		return Procedure.Success;
	}

	public <TArgument extends Bean, TResult extends Bean> TaskCompletionSource<RaftRpc<TArgument, TResult>> SendForWait(
			RaftRpc<TArgument, TResult> rpc) {
		return SendForWait(rpc, false);
	}

	public <TArgument extends Bean, TResult extends Bean> TaskCompletionSource<RaftRpc<TArgument, TResult>> SendForWait(
			RaftRpc<TArgument, TResult> rpc, boolean urgent) {
		if (PendingLimit > 0 && Pending.size() > PendingLimit) // UrgentPending不限制。
			throw new RuntimeException("too many pending");
		// 由于interface不能把setter弄成保护的，实际上外面可以修改。
		// 简单检查一下吧。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(UniqueRequestIdGenerator.next());
		// 外面在发送前可以设置clientId
		if (rpc.getUnique().getClientId().isEmpty())
			rpc.getUnique().setClientId(UniqueRequestIdGenerator.getName());
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0) // set default timeout
			rpc.setTimeout(RaftConfig.getAppendEntriesTimeout());

		var future = new TaskCompletionSource<RaftRpc<TArgument, TResult>>();
		rpc.setUrgent(urgent);
		var pending = urgent ? UrgentPending : Pending;
		rpc.Future = future;
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);

		rpc.setResponseHandle(p -> SendForWaitHandle(p, rpc));
		ConnectorEx leader = _Leader;
		if (!rpc.Send(leader != null ? leader.TryGetReadySocket() : null))
			logger.warn("Send failed: leader={}, rpc={}", leader, rpc);
		return future;
	}

	public static class ConnectorEx extends Connector {
		public ConnectorEx(String host) {
			this(host, 0);
		}

		public ConnectorEx(String host, int port) {
			super(host, port);
		}
	}

	public void Stop() throws Throwable {
		mutex.lock();
		try {
			if (Client == null)
				return;

			Client.Stop();
			Client = null;

			_Leader = null;
			Pending.clear();
			UrgentPending.clear();
		} finally {
			mutex.unlock();
		}
	}

	public Agent(String name, Application zeze) throws Throwable {
		this(name, zeze, null);
	}

	public Agent(String name, Application zeze, RaftConfig raftConf) throws Throwable {
		UniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		Init(new NetClient(this, name, zeze), raftConf);
	}

	public Agent(String name, RaftConfig raftConf) throws Throwable {
		this(name, raftConf, null);
	}

	public Agent(String name, RaftConfig raftConf, Zeze.Config config) throws Throwable {
		if (config == null)
			config = Config.Load();

		UniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		Init(new NetClient(this, name, config), raftConf);
	}

	private void Init(NetClient client, RaftConfig raftConf) throws Exception {
		if (raftConf == null)
			raftConf = Zeze.Raft.RaftConfig.Load();

		RaftConfig = raftConf;
		Client = client;

		if (Client.getConfig().AcceptorCount() != 0)
			throw new IllegalStateException("Acceptor Found!");
		if (Client.getConfig().ConnectorCount() != 0)
			throw new IllegalStateException("Connector Found!");

		for (var node : RaftConfig.getNodes().values())
			Client.getConfig().AddConnector(new ConnectorEx(node.getHost(), node.getPort()));

		Client.AddFactoryHandle(LeaderIs.TypeId_,
				new Service.ProtocolFactoryHandle<>(LeaderIs::new, this::ProcessLeaderIs));

		// ugly
		Task.schedule(1000, 1000, this::ReSend);
	}

	private Connector GetRandomConnector(Connector except) throws Throwable {
		var notMe = new ArrayList<Connector>(Client.getConfig().ConnectorCount());
		Client.getConfig().ForEachConnector(c -> {
			if (c != except)
				notMe.add(c);
		});
		return notMe.isEmpty() ? null : notMe.get(Random.getInstance().nextInt(notMe.size()));
	}

	private long ProcessLeaderIs(LeaderIs r) throws Throwable {
		ConnectorEx leader = _Leader;
		logger.info("=============== LEADERIS Old={} New={} From={}",
				leader != null ? leader.getName() : null, r.Argument.getLeaderId(), r.getSender());

		var node = Client.getConfig().FindConnector(r.Argument.getLeaderId());
		if (node == null) {
			// 当前 Agent 没有 Leader 的配置，创建一个。
			// 由于 Agent 在新增 node 时也会得到新配置广播，
			// 一般不会发生这种情况。
			var address = r.Argument.getLeaderId().split(":");
			if (address.length != 2)
				return 0;

			OutObject<Connector> outNode = new OutObject<>();
			if (Client.getConfig().TryGetOrAddConnector(address[0], Integer.parseInt(address[1]), true, outNode))
				outNode.Value.Start();
		} else if (!r.Argument.isLeader() && r.Argument.getLeaderId().equals(r.getSender().getConnector().getName())) {
			// 【错误处理】用来观察。
			logger.warn("New Leader Is Not A Leader.");
			// 发送者不是Leader，但它的发送的LeaderId又是自己，【尝试选择另外一个Node】。
			node = GetRandomConnector(node);
		}

		if (SetLeader(r, node instanceof ConnectorEx ? (ConnectorEx)node : null))
			ReSend(true);
		// OnLeaderChanged?.Invoke(this);
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private void ReSend() {
		ReSend(false);
	}

	public void CancelPending() {
		// 不包括UrgentPending
		if (Pending.size() == 0)
			return;

		var removed = new ArrayList<RaftRpc<?, ?>>();
		// Pending存在并发访问，这样写更可靠。
		for (var rpc : Pending) {
			var r= Pending.remove(rpc.getUnique().getRequestId());
			if (null != r)
				removed.add(r);
		}
		logger.debug("Found {} RaftRpc cancel", removed.size());
		if (DispatchProtocolToInternalThreadPool)
			Task.getCriticalThreadPool().execute(() -> trigger(removed, "Cancel"));
		else
			Task.run(() -> trigger(removed, "Cancel"), "Trigger Timeout RaftRpcs");
	}

	private void ReSend(boolean immediately) {
		ConnectorEx leader = _Leader;
		if (leader != null)
			leader.Start();
		// ReSendPendingRpc
		var leaderSocket = leader != null ? leader.TryGetReadySocket() : null;
		ArrayList<RaftRpc<?, ?>> removed = null;
		long now = System.currentTimeMillis();
		long timeout = RaftConfig.getAppendEntriesTimeout() + 1000;
		for (var rpc : UrgentPending) {
			if (rpc.getTimeout() > 0 && now - rpc.getCreateTime() > rpc.getTimeout()) {
				rpc = UrgentPending.remove(rpc.getUnique().getRequestId());
				if (rpc != null) {
					if (removed == null)
						removed = new ArrayList<>();
					removed.add(rpc);
				}
				continue;
			}
			if (immediately || now - rpc.getSendTime() > timeout) {
				logger.debug("ReSendU {}/{} {}", UrgentPending.size(), leaderSocket, rpc);
				rpc.setSendTime(now);
				if (!rpc.Send(leaderSocket)) {
					logger.info("SendRequest failed {}", rpc);
					break;
				}
			}
		}
		for (var rpc : Pending) {
			if (rpc.getTimeout() > 0 && now - rpc.getCreateTime() > rpc.getTimeout()) {
				rpc = Pending.remove(rpc.getUnique().getRequestId());
				if (rpc != null) {
					if (removed == null)
						removed = new ArrayList<>();
					removed.add(rpc);
				}
				continue;
			}
			if (immediately || now - rpc.getSendTime() > timeout) {
				logger.debug("ReSend {}/{} {}", Pending.size(), leaderSocket, rpc);
				rpc.setSendTime(now);
				if (!rpc.Send(leaderSocket)) {
					logger.info("SendRequest failed {}", rpc);
					break;
				}
			}
		}
		if (removed != null) {
			logger.debug("Found {} RaftRpc timeout", removed.size());
			var removed0 = removed;
			if (DispatchProtocolToInternalThreadPool)
				Task.getCriticalThreadPool().execute(() -> trigger(removed0));
			else
				Task.run(() -> trigger(removed0), "Trigger Timeout RaftRpcs");
		}
	}

	private void trigger(ArrayList<RaftRpc<?, ?>> removed) {
		trigger(removed, "Timeout");
	}

	private void trigger(ArrayList<RaftRpc<?, ?>> removed, String reason) {
		for (var r : removed) {
			if (null == r)
				continue;
			r.setIsTimeout(true);
			if (null != r.Future) {
				r.Future.SetException(new RuntimeException(reason));
			} else {
				try {
					r.Handle.applyAsLong(r);
				} catch (Throwable e) {
					logger.error("", e);
				}
			}
		}
	}

	public boolean SetLeader(LeaderIs r, ConnectorEx newLeader) throws Throwable {
		mutex.lock();
		try {
			if (r.Argument.getTerm() < Term) {
				logger.warn("Skip LeaderIs {} {}", newLeader.getName(), r);
				return false;
			}

			_Leader = newLeader; // change current Leader
			Term = r.Argument.getTerm();
			if (newLeader != null)
				newLeader.Start(); // try connect immediately
			Action1<Agent> onSetLeader = OnSetLeader;
			if (onSetLeader != null)
				onSetLeader.run(this);
			return true;
		} finally {
			mutex.unlock();
		}
	}

	public static final class NetClient extends HandshakeClient {
		private final Agent Agent;

		public NetClient(Agent agent, String name, Application zeze) throws Throwable {
			super(name, zeze);
			Agent = agent;
		}

		public NetClient(Agent agent, String name, Zeze.Config config) throws Throwable {
			super(name, config);
			Agent = agent;
		}

		public Agent getAgent() {
			return Agent;
		}

		@Override
		public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			// Raft RPC 的回复处理应该都不是block的,直接在IO线程处理,避免线程池堆满等待又无法唤醒导致死锁
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) {
				logger.error("Agent.NetClient.DispatchRpcResponse", e);
			}
		}

		@Override
		public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> pfh) {
			// 虚拟线程创建太多Critical线程反而容易卡,以后考虑跑另个虚拟线程池里
			if ((p.getTypeId() == LeaderIs.TypeId_ || Agent.DispatchProtocolToInternalThreadPool) && !Task.isVirtualThreadEnabled())
				Task.getCriticalThreadPool().execute(() -> Task.Call(() -> pfh.Handle.handle(p), "InternalRequest"));
			else
				Task.run(() -> pfh.Handle.handle(p), p);
		}
	}
}
