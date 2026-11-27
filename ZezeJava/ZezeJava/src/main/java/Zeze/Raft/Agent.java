package Zeze.Raft;

import java.util.Objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToLongFunction;
import Zeze.Application;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.RpcTimeoutException;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.ConcurrentHashMapOrdered;
import Zeze.Util.Func3;
import Zeze.Util.OutObject;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskCompletionSourceX;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Raft 客户端 Agent：维护到 Leader 的连接与 LeaderIs 跟踪、唯一请求发号、pending rpc 的
 * 周期重发与超时闭环；可选经 ProxyAgent 走共享代理连接。
 */
public final class Agent {
	private static final Logger logger = LogManager.getLogger(Agent.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	// 持久化自增发号器，避免短时间重启导致 UniqueRequestId 重复。
	private final PersistentAtomicLong uniqueRequestIdGenerator;
	// 可选的 clientId 覆盖（null 时默认使用发号器名），供临时 Agent 获得进程级唯一 clientId。
	private volatile @Nullable String uniqueClientIdOverride;
	private RaftConfig raftConfig;
	private @Nullable NetClient client;
	private volatile @Nullable ConnectorProxy leader;
	private final ConcurrentHashMapOrdered<Long, RaftRpc<?, ?>> pending = new ConcurrentHashMapOrdered<>();
	private volatile long term; // 在 mutex 内写入，volatile 保证 getTerm() 锁外读可见性
	/** 入站协议默认投入内部线程池执行；显式注册 DispatchMode.Direct 的协议不受此标志影响。 */
	public boolean dispatchProtocolToInternalThreadPool;
	private volatile int pendingLimit = -1; // -1 表示不限制（非严格并发保护）
	private @Nullable Future<?> resendTask;
	// stop 时置位且不可复位；配合 send/sendForWait 注册 pending 后的二次检查，保证所有已注册请求都能被排空或补完成。
	private volatile boolean stopped;
	// 一次性重发窗口配置告警标志
	private final AtomicBoolean warnedResendNeverTrigger = new AtomicBoolean();

	private @Nullable Action1<Agent> onSetLeader;
	private final Lock mutex = new ReentrantLock();
	private final @Nullable ProxyAgent proxyAgent;

	public RaftConfig getRaftConfig() {
		return raftConfig;
	}

	public int getPendingLimit() {
		return pendingLimit;
	}

	public void setPendingLimit(int value) {
		pendingLimit = value;
	}

	public @Nullable NetClient getClient() {
		return client;
	}

	public String getName() {
		//noinspection DataFlowIssue
		return client.getName();
	}

	public @Nullable ConnectorProxy getLeader() {
		return leader;
	}

	public long getTerm() {
		return term;
	}

	public @Nullable Action1<Agent> getOnSetLeader() {
		return onSetLeader;
	}

	public void setOnSetLeader(@Nullable Action1<Agent> onSetLeader) {
		this.onSetLeader = onSetLeader;
	}

	/**
	 * 发送Rpc请求。
	 */
	public <TArgument extends Serializable, TResult extends Serializable>
	void send(RaftRpc<TArgument, TResult> rpc, @Nullable ToLongFunction<Protocol<?>> handle) {
		if (handle == null)
			throw new IllegalArgumentException("null handle");
		if (pendingLimit > 0 && pending.size() > pendingLimit) // UrgentPending不限制。
			throw new IllegalStateException("too many pending");

		// 防护外部复用已发号或已篡改的 RaftRpc。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(uniqueRequestIdGenerator.next());
		// 外面可以设置clientId，默认使用Generator.getName();
		if (rpc.getUnique().getClientId().isEmpty()) {
			var clientId = uniqueClientIdOverride;
			rpc.getUnique().setClientId(clientId != null ? clientId : uniqueRequestIdGenerator.getName());
		}
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0)
			rpc.setTimeout(raftConfig.getAgentTimeout());
		else
			checkResendWindow(rpc);

		rpc.handle = handle;
		rpc.setResponseHandle(p -> sendHandle(p, rpc));
		rpc.freezeRequestState();
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);
		// 若并发 stop 已置位，由发送方兜底触发超时回调，与 stop 的排空经 pending.remove 互斥。
		if (stopped) {
			if (pending.remove(rpc.getUnique().getRequestId()) != null)
				trigger(List.of(rpc), "AgentStopped");
			return;
		}
		ConnectorProxy leader = this.leader;
		if (!ProxyAgent.send(client, proxyAgent, rpc, leader,
				leader != null ? leader.getConnector().TryGetReadySocket() : null))
			logger.debug("send failed: leader={}, rpc={}", leader, rpc);
	}

	private <TArgument extends Serializable, TResult extends Serializable>
	long sendHandle(Rpc<TArgument, TResult> p, RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || isRetryError(net.getResultCode()))
			return Procedure.Success; // 由 pending 定时重发

		long requestId = rpc.getUnique().getRequestId();
		if (pending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			if (isDebugEnabled) {
				logger.debug("sendHandle Rpc={} RequestId={} ResultCode={} Sender={}",
						rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			}
			//noinspection DataFlowIssue
			return rpc.handle.applyAsLong(rpc);
		}
		return Procedure.Success;
	}

	private static boolean isRetryError(long error) {
		return error == Procedure.CancelException ||
				error == Procedure.RaftRetry ||
				error == Procedure.DuplicateRequest;
	}

	@SuppressWarnings("SameReturnValue")
	private <TArgument extends Serializable, TResult extends Serializable>
	long sendForWaitHandle(Rpc<TArgument, TResult> p, RaftRpc<TArgument, TResult> rpc) {
		var net = (RaftRpc<TArgument, TResult>)p;
		if (net.isTimeout() || isRetryError(net.getResultCode()))
			return Procedure.Success; // 由 pending 定时重发

		long requestId = rpc.getUnique().getRequestId();
		if (pending.remove(requestId) != null) {
			rpc.setRequest(net.isRequest());
			rpc.Result = net.Result;
			rpc.setSender(net.getSender());
			rpc.setResultCode(net.getResultCode());

			if (rpc.getResultCode() == Procedure.RaftApplied)
				rpc.setIsTimeout(false);
			if (isDebugEnabled) {
				logger.debug("sendForWaitHandle Rpc={} RequestId={} ResultCode={} Sender={}",
						rpc.getClass().getSimpleName(), requestId, rpc.getResultCode(), rpc.getSender());
			}
			//noinspection DataFlowIssue
			rpc.future.setResult(rpc);
		}
		return Procedure.Success;
	}

	public <TArgument extends Serializable, TResult extends Serializable>
	TaskCompletionSourceX<RaftRpc<TArgument, TResult>> sendForWait(RaftRpc<TArgument, TResult> rpc) {
		if (pendingLimit > 0 && pending.size() > pendingLimit) // UrgentPending不限制。
			throw new IllegalStateException("too many pending");
		// 防护外部复用已发号或已篡改的 RaftRpc。
		if (rpc.getUnique().getRequestId() != 0)
			throw new IllegalStateException("RaftRpc.UniqueRequestId != 0. Need A Fresh RaftRpc");

		rpc.getUnique().setRequestId(uniqueRequestIdGenerator.next());
		// 外面在发送前可以设置clientId
		if (rpc.getUnique().getClientId().isEmpty()) {
			var clientId = uniqueClientIdOverride;
			rpc.getUnique().setClientId(clientId != null ? clientId : uniqueRequestIdGenerator.getName());
		}
		rpc.setCreateTime(System.currentTimeMillis());
		rpc.setSendTime(rpc.getCreateTime());
		if (rpc.getTimeout() == 0)
			rpc.setTimeout(raftConfig.getAgentTimeout());
		else
			checkResendWindow(rpc);

		var future = new TaskCompletionSourceX<RaftRpc<TArgument, TResult>>();
		rpc.future = future;
		rpc.setResponseHandle(p -> sendForWaitHandle(p, rpc));
		rpc.freezeRequestState();
		if (pending.putIfAbsent(rpc.getUnique().getRequestId(), rpc) != null)
			throw new IllegalStateException("duplicate requestId rpc=" + rpc);
		// 若并发 stop 已置位，立即异常完成 future 避免等待者永久挂起。
		if (stopped) {
			if (pending.remove(rpc.getUnique().getRequestId()) != null)
				trigger(List.of(rpc), "AgentStopped");
			return future;
		}
		ConnectorProxy leader = this.leader;
		if (!ProxyAgent.send(client, proxyAgent, rpc, leader,
				leader != null ? leader.getConnector().TryGetReadySocket() : null))
			logger.debug("sendForWait failed: leader={}, rpc={}", leader, rpc);
		return future;
	}

	/**
	 * Connector 代理，
	 * 1. 当没有启用代理时，它和里面指向的connector一一对应。
	 * 2. 当启用代理时，它和里面指向的connector是多对一的关系。
	 */
	public static class ConnectorProxy {
		private final @Nullable String raftName;
		private final Connector connector;

		public String getName() {
			return (null == raftName || raftName.isBlank()) ? connector.getName() : raftName;
		}

		public Connector getConnector() {
			return connector;
		}

		/**
		 * 启用代理构造函数
		 *
		 * @param raftName  raftName
		 * @param connector connector
		 */
		public ConnectorProxy(String raftName, Connector connector) {
			this.raftName = raftName;
			this.connector = connector;
		}

		/**
		 * 没有启用代理构造函数
		 *
		 * @param connector connector
		 */
		public ConnectorProxy(Connector connector) {
			this.raftName = null;
			this.connector = connector;
		}
	}

	public void stop() throws Exception {
		Future<?> toCancel = null;
		mutex.lock(); // stop 不中断
		try {
			// 先置 stopped 再排空 pending，与 send/sendForWait 的注册后检查配对封住竞态窗口。
			stopped = true;
			if (resendTask != null) {
				toCancel = resendTask;
				resendTask = null;
			}
			if (client == null)
				return;

			// 尽力完成全部清理与 pending 排空，单点失败不中断后续收尾。
			try {
				client.stop();
			} catch (Throwable e) {
				logger.error("Agent.stop client.stop fail, name={}", client.getName(), e);
			}
			client = null;

			if (null != proxyAgent) {
				try {
					proxyAgent.removeAgent(this);
				} catch (Throwable e) {
					logger.error("Agent.stop removeAgent fail", e);
				}
			}

			leader = null;

			// 先逐条原子摘除再触发回调，避免与 IO 线程真实应答并发双触发。
			var removed = new ArrayList<RaftRpc<?, ?>>();
			for (var rpc : pending) {
				var r = pending.remove(rpc.getUnique().getRequestId());
				if (null != r)
					removed.add(r);
			}
			try {
				trigger(removed, "stopPending");
			} catch (Throwable e) {
				logger.error("Agent.stop trigger pending fail", e);
			}
		} finally {
			mutex.unlock();
			// 先释放 mutex 再取消 resendTask，避免与正在执行且等待 mutex 的 resend 死锁。
			if (toCancel != null)
				toCancel.cancel(true);
		}
	}

	public Agent(String name, Application zeze) throws Exception {
		this(name, zeze, null);
	}

	public Agent(String name, Application zeze, @Nullable RaftConfig raftConf) throws Exception {
		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		this.proxyAgent = null;
		init(new NetClient(this, name, zeze), raftConf);
	}

	public Agent(String name, Application zeze, @Nullable RaftConfig raftConf,
				 Func3<Agent, String, Application, NetClient> netClientFactory) throws Exception {
		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		this.proxyAgent = null;
		init(netClientFactory.call(this, name, zeze), raftConf);
	}

	public Agent(String name, Application zeze, @Nullable RaftConfig raftConf,
				 Func3<Agent, String, Application, NetClient> netClientFactory,
				 @Nullable ProxyAgent proxyAgent) throws Exception {
		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + '.' + zeze.getConfig().getServerId());
		this.proxyAgent = proxyAgent;
		init(netClientFactory.call(this, name, zeze), raftConf);
	}

	public Agent(String name, @Nullable RaftConfig raftConf) throws Exception {
		this(name, raftConf, null);
	}

	public Agent(String name, @Nullable RaftConfig raftConf, @Nullable Config config) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		this.proxyAgent = null;
		init(new NetClient(this, name, config), raftConf);
	}

	public Agent(String name, @Nullable RaftConfig raftConf, @Nullable Config config,
				 Func3<Agent, String, Config, NetClient> netClientFactory) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		this.proxyAgent = null;
		init(netClientFactory.call(this, name, config), raftConf);
	}

	public Agent(String name, @Nullable RaftConfig raftConf, @Nullable Config config,
				 @Nullable ProxyAgent proxyAgent) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		this.proxyAgent = proxyAgent;
		init(new NetClient(this, name, config), raftConf);
	}

	public Agent(String name, @Nullable RaftConfig raftConf, @Nullable Config config,
				 Func3<Agent, String, Config, NetClient> netClientFactory,
				 @Nullable ProxyAgent proxyAgent) throws Exception {
		if (config == null)
			config = Config.load();

		uniqueRequestIdGenerator = PersistentAtomicLong.getOrAdd(name + ',' + config.getServerId());
		this.proxyAgent = proxyAgent;
		init(netClientFactory.call(this, name, config), raftConf);
	}

	private void init(NetClient client, @Nullable RaftConfig raftConf) throws Exception {
		if (raftConf == null)
			raftConf = RaftConfig.load();

		raftConfig = raftConf;
		this.client = client;
		try {
			initClient();
		} catch (Exception | Error e) {
			try {
				stop();
			} catch (Exception | Error cleanup) {
				e.addSuppressed(cleanup);
			}
			throw e;
		}
	}

	private void initClient() {
		var client = Objects.requireNonNull(this.client);
		if (client.getConfig().acceptorCount() != 0)
			throw new IllegalStateException("Acceptor Found!");
		if (client.getConfig().connectorCount() != 0)
			throw new IllegalStateException("Connector Found!");

		// AppendEntriesTimeout >= 5000ms 时默认超时 rpc 的重发窗口偏紧，给出一次性提示。
		if (raftConfig.getAppendEntriesTimeout() >= 5000)
			logger.warn("AppendEntriesTimeout({}ms) >= Rpc default timeout(5000ms):"
							+ " rpcs without explicit timeout resend at a tight interval"
							+ " (timeout - {}ms) before their deadline.",
					raftConfig.getAppendEntriesTimeout(), resendScanPeriodMs);

		if (proxyAgent == null) {
			// 未启用代理，直接为各节点建立直连 Connector。
			for (var node : raftConfig.getNodes().values())
				client.getConfig().addConnector(new Connector(node.getHost(), node.getPort()));
		} else {
			proxyAgent.addAgent(this);
			leader = proxyAgent.getLeader(raftConfig.getNodes().values().iterator().next());
			if (leader == null)
				throw new IllegalStateException("proxy leader not found, node not configured with proxy: "
						+ raftConfig.getNodes().values().iterator().next().getName());
			//noinspection DataFlowIssue
			logger.info("proxy first leader {} {}_{}",
					leader.getName(), leader.getConnector().getHostNameOrAddress(), leader.getConnector().getPort());
			// 预先建立全部代理节点连接，供后续 GetLeader 首发现使用。
			for (var node : raftConfig.getNodes().values())
				proxyAgent.getLeader(node);
		}

		client.AddFactoryHandle(LeaderIs.TypeId_, new Service.ProtocolFactoryHandle<>(
				LeaderIs::new, this::processLeaderIs, TransactionLevel.Serializable, DispatchMode.Critical));

		client.AddFactoryHandle(GetLeader.TypeId_, new Service.ProtocolFactoryHandle<>(
				GetLeader::new, null, TransactionLevel.None, DispatchMode.Normal));
		client.AddFactoryHandle(StartServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
				StartServerConnector::new, null, TransactionLevel.None, DispatchMode.Normal));
		client.AddFactoryHandle(StopServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
				StopServerConnector::new, null, TransactionLevel.None, DispatchMode.Normal));
		// 代理模式下在协议工厂注册完成后主动并行查询 Leader，加速首个请求命中真实 Leader。
		if (proxyAgent != null)
			discoverLeaderByGetLeader();
		resendTask = TaskSpec.ofAction(this::resend).schedulePeriodNow(1000, 1000);
	}

	// 代理模式首发现超时预算；耗尽无权威应答时在 ProxyAgent 标记短窗免疫。
	private static final int proxyLeaderDiscoverTimeoutMs = 1000;

	/**
	 * 代理模式首发现：向各已就绪代理节点并行发送 GetLeader，收到首个权威应答即切换 Leader；
	 * 若均无权威应答则保留首节点猜测，交由后续 redirect 与 resend 机制纠偏。
	 */
	private void discoverLeaderByGetLeader() {
		if (proxyAgent.isDiscoverImmune())
			return;
		long deadline = System.currentTimeMillis() + proxyLeaderDiscoverTimeoutMs;
		long firstSocketGrace = deadline - 700; // 首条就绪连接宽限 300ms
		// 无权威应答哨兵（term < 0）
		var noLeader = new LeaderIs();
		noLeader.Argument.setTerm(-1);
		var future = new TaskCompletionSource<LeaderIs>();
		var queried = new HashSet<String>();
		var pending = new AtomicInteger();
		try {
			while (true) {
				for (var node : raftConfig.getNodes().values()) {
					if (!queried.add(node.getName()))
						continue;
					var nodeProxy = proxyAgent.getLeader(node);
					if (nodeProxy == null)
						continue;
					var socket = nodeProxy.getConnector().TryGetReadySocket();
					if (socket == null) {
						queried.remove(node.getName()); // 未就绪，留待下轮补发
						continue;
					}
					pending.incrementAndGet();
					var get = new GetLeader();
					get.setResponseHandle(p -> {
						var g = (GetLeader)p; // 代理应答为隧道解码的新实例，需从回调参数 p 读取
						if (g.getResultCode() == Procedure.Success) {
							var leaderId = g.Result.getLeaderId();
							var leaderNode = leaderId == null || leaderId.isEmpty() ? null : raftConfig.getNodes().get(leaderId);
							if (leaderNode != null) {
								var r = new LeaderIs();
								r.Argument.setTerm(g.Result.getTerm());
								r.Argument.setLeaderId(leaderId);
								r.Argument.setLeader(g.Result.isLeader());
								future.setResult(r);
							}
						}
						pending.decrementAndGet();
						return 0;
					});
					ProxyAgent.send(client, proxyAgent, get, nodeProxy, socket);
				}
				var r = future.getNow(null);
				if (r != null)
					break;
				if (queried.size() == raftConfig.getNodes().size() && pending.get() == 0) {
					future.setResult(noLeader); // 全部节点问完仍无 Leader（如选举中），快速结束等待
					break;
				}
				var now = System.currentTimeMillis();
				if (now >= deadline)
					break;
				if (pending.get() == 0 && now >= firstSocketGrace)
					break; // 宽限内无可用连接，提前结束
				future.await(20);
			}
			var r = future.getNow(null);
			if (r == null || r.Argument.getTerm() < 0) {
				// 预算耗尽无应答时标记短窗免疫；全部负应答（选举中）不免疫。
				if (r == null)
					proxyAgent.markDiscoverNoAnswer();
				return;
			}
			var newLeader = proxyAgent.getLeader(Objects.requireNonNull(raftConfig.getNodes().get(r.Argument.getLeaderId())));
			proxyAgent.clearDiscoverImmunity();
			setLeader(r, newLeader);
		} catch (Throwable e) { // 首发现失败时回退至默认首节点猜测
			logger.warn("proxy leader discover fail. name={}", getName(), e);
		}
	}

	private @Nullable Connector getRandomConnector(@Nullable Connector except) {
		//noinspection DataFlowIssue
		var notMe = new ArrayList<Connector>(client.getConfig().connectorCount());
		client.getConfig().forEachConnector(c -> {
			if (c != except)
				notMe.add(c);
		});
		return notMe.isEmpty() ? null : notMe.get(Random.getInstance().nextInt(notMe.size()));
	}

	private long processLeaderIs(LeaderIs r) throws Exception {
		mutex.lock();
		try {
			if (stopped)
				return Procedure.Success;
			return processLeaderIsLocked(r);
		} finally {
			mutex.unlock();
		}
	}

	// 调用者持 mutex，与 stop 串行：连接启动及 Leader 发布不能越过停止边界。
	private long processLeaderIsLocked(LeaderIs r) throws Exception {
		ConnectorProxy leader = this.leader;
		logger.info("=============== LEADERS Old={} New={} From={}",
				leader != null ? leader.getName() : null, r.Argument.getLeaderId(), r.getSender());

		if (null != proxyAgent) {
			var leaderNode = raftConfig.getNodes().get(r.Argument.getLeaderId());
			if (null == leaderNode)
				return 0; // 本地尚无该节点配置，待配置同步后经重定向恢复
			var newLeader = proxyAgent.getLeader(leaderNode);
			if (null != newLeader) {
				if (setLeader(r, newLeader))
					resend(true);

				r.SendResultCode(0);
				return Procedure.Success;
			}
		}

		//noinspection DataFlowIssue
		var node = client.getConfig().findConnector(Objects.requireNonNull(r.Argument.getLeaderId()));
		if (node == null) {
			// 本地缺少该 Leader 的连接器时按地址动态补充。
			var address = r.Argument.getLeaderId().split("_");
			if (address.length != 2)
				return 0;

			OutObject<Connector> outNode = new OutObject<>();
			if (client.getConfig().tryGetOrAddConnector(address[0], Integer.parseInt(address[1]), true, outNode))
				//noinspection DataFlowIssue
				outNode.value.start();
			node = outNode.value;
		} else {
			// 代理隧道解码的 rpc 无 sender，跳过自指校验。
			//noinspection DataFlowIssue
			if (r.getSender() != null && !r.Argument.isLeader() && r.Argument.getLeaderId().equals(r.getSender().getConnector().getName())) {
				logger.warn("New Leader Is Not A Leader.");
				// 对端声称自己是 Leader 但 isLeader 为 false，改选其他节点。
				node = getRandomConnector(node);
			}
		}

		if (setLeader(r, new ConnectorProxy(Objects.requireNonNull(node))))
			resend(true);

		r.SendResultCode(0);
		return Procedure.Success;
	}

	private void resend() {
		resend(false);
	}

	// 扫描周期与 resendTask 一致；单请求重发间隔取 min(AppendEntriesTimeout, rpcTimeout - 扫描周期)，
	// 确保超时大于扫描周期的 rpc 在判死前至少经历一次同号重发。
	private static final int resendScanPeriodMs = 1000;

	static long resendIntervalOf(long appendEntriesTimeout, int rpcTimeout) {
		return rpcTimeout > 0 ? Math.min(appendEntriesTimeout, rpcTimeout - resendScanPeriodMs) : appendEntriesTimeout;
	}

	private void checkResendWindow(RaftRpc<?, ?> rpc) {
		if (rpc.getTimeout() > 0 && rpc.getTimeout() <= raftConfig.getAppendEntriesTimeout()
				&& warnedResendNeverTrigger.compareAndSet(false, true)) {
			logger.warn("RaftRpc timeout({}ms) <= AppendEntriesTimeout({}ms): tight resend interval"
							+ " (createTime based deadline) applies for such rpc."
							+ " Set rpc timeout=0 (AgentTimeout={}) or > AppendEntriesTimeout for normal cadence. rpc={}",
					rpc.getTimeout(), raftConfig.getAppendEntriesTimeout(), raftConfig.getAgentTimeout(), rpc);
		}
	}

	public void cancelPending() {
		// 不包括UrgentPending
		if (pending.isEmpty())
			return;

		var removed = new ArrayList<RaftRpc<?, ?>>();
		// Pending存在并发访问，这样写更可靠。
		for (var rpc : pending) {
			var r = pending.remove(rpc.getUnique().getRequestId());
			if (null != r)
				removed.add(r);
		}
		if (isDebugEnabled)
			logger.debug("Found {} RaftRpc cancel", removed.size());
		//noinspection DataFlowIssue
		Task.getCriticalThreadPool().execute(() -> trigger(removed, "Cancel"));
	}

	private void resend(boolean immediately) {
		mutex.lock();
		try {
			if (stopped)
				return;
			resendLocked(immediately);
		} finally {
			mutex.unlock();
		}
	}

	// 调用者持 mutex，与 stop 串行：连接启动及 Leader 发布不能越过停止边界。
	private void resendLocked(boolean immediately) {
		ConnectorProxy leader = this.leader;
		if (leader != null)
			leader.getConnector().start();
		var leaderSocket = leader != null ? leader.getConnector().TryGetReadySocket() : null;
		ArrayList<RaftRpc<?, ?>> removed = null;
		long now = System.currentTimeMillis();
		// 以 AppendEntriesTimeout 为基准计算重发间隔，服务端按 UniqueRequestId 保证重发幂等。
		long timeout = raftConfig.getAppendEntriesTimeout();
		boolean canSend = true;
		for (var rpc : pending) {
			if (rpc.getTimeout() > 0 && now - rpc.getCreateTime() > rpc.getTimeout()) {
				var expired = pending.remove(rpc.getUnique().getRequestId());
				if (expired != null) {
					if (removed == null)
						removed = new ArrayList<>();
					removed.add(expired);
				}
				continue;
			}
			long resendInterval = resendIntervalOf(timeout, rpc.getTimeout());
			if (canSend && ((immediately && now - rpc.getCreateTime() > resendInterval)
					|| now - rpc.getSendTime() > resendInterval)) {
				if (isDebugEnabled)
					logger.debug("ReSend {}/{} {}", pending.size(), leaderSocket, rpc);
				rpc.setSendTime(now);
				if (!ProxyAgent.send(client, proxyAgent, rpc, leader, leaderSocket)) {
					logger.info("SendRequest failed {}", rpc);
					// 停止本轮发送，但继续回收后续已超时的请求。
					canSend = false;
				}
			}
		}
		if (removed != null) {
			if (isDebugEnabled)
				logger.debug("Found {} RaftRpc timeout", removed.size());
			var removed0 = removed;
			//noinspection DataFlowIssue
			Task.getCriticalThreadPool().execute(() -> trigger(removed0));
		}
	}

	private static void trigger(Iterable<? extends @Nullable RaftRpc<?, ?>> removed) {
		trigger(removed, "Timeout");
	}

	private static void trigger(Iterable<? extends @Nullable RaftRpc<?, ?>> removed, String reason) {
		for (var r : removed) {
			if (null == r)
				continue;
			r.setIsTimeout(true);
			// 同时设置 isTimeout 与 Timeout 错误码，保证仅检查 resultCode 的回调路径也能感知超时。
			r.setResultCode(Procedure.Timeout);
			if (null != r.future) {
				r.future.setException(new RpcTimeoutException(reason));
			} else {
				try {
					//noinspection DataFlowIssue
					r.handle.applyAsLong(r);
				} catch (Throwable e) {
					logger.error("", e);
				}
			}
		}
	}

	public boolean setLeader(LeaderIs r, ConnectorProxy newLeader) throws Exception {
		mutex.lock();
		try {
			if (stopped)
				return false;
			if (r.Argument.getTerm() < term) {
				logger.warn("Skip LeaderIs {} {}", newLeader.getName(), r);
				return false;
			}

			//noinspection DataFlowIssue
			logger.info("proxy set leader {} {}_{}->{} {}_{}",
					null != leader ? leader.getName() : "",
					null != leader ? leader.getConnector().getHostNameOrAddress() : "",
					null != leader ? leader.getConnector().getPort() : "",
					newLeader.getName(),
					newLeader.getConnector().getHostNameOrAddress(),
					newLeader.getConnector().getPort());
			leader = newLeader;
			term = r.Argument.getTerm();
			newLeader.getConnector().start();
			Action1<Agent> onSetLeader = this.onSetLeader;
			if (onSetLeader != null)
				onSetLeader.run(this);
			return true;
		} finally {
			mutex.unlock();
		}
	}

	public static class NetClient extends HandshakeClient {
		private final Agent agent;

		public NetClient(Agent agent, String name, Application zeze) {
			super(name, zeze);
			this.agent = agent;
		}

		public NetClient(Agent agent, String name, Config config) {
			super(name, config);
			this.agent = agent;
		}

		public Agent getAgent() {
			return agent;
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			// Raft RPC 的回复处理应是非阻塞的，直接在 IO 线程执行以避免线程池满载导致无法唤醒。
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) {
				logger.error("Agent.NetClient.dispatchRpcResponse", e);
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) throws Exception {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.dispatch(this, factoryHandle);
		}

		@Override
		public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
			// Direct 优先于池化（含 dispatchProtocolToInternalThreadPool），保证显式声明串行的协议在调用线程按序执行。
			if (factoryHandle.Mode != DispatchMode.Direct
					&& (p.getTypeId() == LeaderIs.TypeId_ || isHandshakeProtocol(p.getTypeId()) || agent.dispatchProtocolToInternalThreadPool)) {
				//noinspection DataFlowIssue
				Task.getCriticalThreadPool().execute(() -> TaskSpec.ofFunc(() -> p.handle(this, factoryHandle)).name("InternalRequest").call());
			} else
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
						.dispatchMode(factoryHandle.Mode).runNow();
		}
	}

	public void getLeaderAsync(ToLongFunction<GetLeader> handle) {
		send(new GetLeader(), (p) -> handle.applyAsLong((GetLeader)p));
	}

	// waitForLeader 临时 Agent 复用固定名称的持久化发号器，通过进程级随机 clientId 保证跨重启/跨目录去重唯一。
	private static final String waitForLeaderClientId = "waitForLeader.client." + UUID.randomUUID();

	/** 覆盖对外请求的默认 clientId（不设置时默认使用发号器名）。 */
	public void setUniqueClientId(String clientId) {
		uniqueClientIdOverride = clientId;
	}

	public static @Nullable Connector waitForLeader(RaftConfig raftConfig) {
		return waitForLeader(raftConfig, 0, null);
	}

	public static @Nullable Connector waitForLeader(RaftConfig raftConfig, long timeoutMs) {
		return waitForLeader(raftConfig, timeoutMs, null);
	}

	public static @Nullable Connector waitForLeader(RaftConfig raftConfig, long timeoutMs, @Nullable OutObject<Agent> out) {
		if (raftConfig.getSuggestMajorityCount() < raftConfig.getMajorityCount())
			throw new RuntimeException("suggest majority node too few.");

		try {
			// 固定 Agent 名复用同一发号器文件，避免临时文件堆积。
			var agent = new Agent("waitForLeader", raftConfig);
			agent.setUniqueClientId(waitForLeaderClientId);
			// 创建后即回填 out，供调用方查询连接状态并管理生命周期。
			if (out != null)
				out.value = agent;
			try {
				var future = new TaskCompletionSource<@Nullable Connector>();
				//noinspection DataFlowIssue
				agent.setOnSetLeader((_agent) -> future.setResult(_agent.getLeader().getConnector()));
				//noinspection DataFlowIssue
				agent.getClient().start();
				var netConfig = agent.getClient().getConfig();
				// 主动发起 GetLeader 与 onSetLeader 广播双路竞争唤醒，先到达者生效。
				agent.getLeaderAsync((get) -> {
					if (get.getResultCode() == 0)
						future.setResult(netConfig.findConnector(Objects.requireNonNull(get.Result.getLeaderId())));
					return 0;
				});
				if (timeoutMs <= 0) {
					// 默认超时按一个完整选举周期（含 PreVote）估算。
					timeoutMs = raftConfig.getLeaderHeartbeatTimer()
							+ raftConfig.getElectionTimeoutMax() * 2L
							+ raftConfig.getAppendEntriesTimeout();
				}
				return future.get(timeoutMs, TimeUnit.MILLISECONDS);
			} finally {
				// out 非 null 时由调用方负责停止并回收 agent。
				if (out == null)
					agent.stop();
			}
		} catch (Exception ex) {
			logger.warn("", ex);
			return null;
		}
	}

	public static @Nullable Connector waitForLeader(Agent agent, RaftConfig raftConfig, long timeoutMs) {
		var future = new TaskCompletionSource<@Nullable Connector>();
		//noinspection DataFlowIssue
		agent.setOnSetLeader((_agent) -> future.setResult(_agent.getLeader().getConnector()));
		//noinspection DataFlowIssue
		var netConfig = agent.getClient().getConfig();
		// onSetLeader 与 GetLeader 双路竞争唤醒，先到达者生效。
		agent.getLeaderAsync((get) -> {
			if (get.getResultCode() == 0)
				future.setResult(netConfig.findConnector(Objects.requireNonNull(get.Result.getLeaderId())));
			return 0;
		});
		if (timeoutMs <= 0) {
			// 默认超时按一个完整选举周期（含 PreVote）估算。
			timeoutMs = raftConfig.getLeaderHeartbeatTimer()
					+ raftConfig.getElectionTimeoutMax() * 2L
					+ raftConfig.getAppendEntriesTimeout();
		}
		return future.get(timeoutMs, TimeUnit.MILLISECONDS);
	}

	private void clearExcludedLeader(Set<String> excludeNames) {
		mutex.lock();
		try {
			if (leader != null && excludeNames.contains(leader.getName()))
				leader = null;
		} finally {
			mutex.unlock();
		}
	}

	/**
	 * 等待选出不在 {@code excludeNames} 中的新 Leader：
	 * 先清除指向已排除节点的旧 leader 引用，优先由新 Leader 的 LeaderIs 广播经 onSetLeader 立即唤醒，
	 * 同时以有界周期向未被排除的活跃节点探测 GetLeader 作为兜底，避免停连接后仍向旧 Leader 忙查。
	 */
	static @Nullable Connector waitForLeader(Agent agent, RaftConfig raftConfig,
											 Set<String> excludeNames, long timeoutMs) {
		if (timeoutMs <= 0) {
			// PreVote引入后选举最坏情况多一轮，预算给两倍ElectionTimeoutMax。
			timeoutMs = raftConfig.getLeaderHeartbeatTimer()
					+ raftConfig.getElectionTimeoutMax() * 2L
					+ raftConfig.getAppendEntriesTimeout();
		}
		var future = new TaskCompletionSource<@Nullable Connector>();
		//noinspection DataFlowIssue
		var netConfig = agent.getClient().getConfig();
		agent.setOnSetLeader(_agent -> {
			var current = _agent.getLeader();
			if (current != null && !excludeNames.contains(current.getName()))
				future.setResult(current.getConnector());
		});
		agent.clearExcludedLeader(excludeNames);

		long deadline = System.currentTimeMillis() + timeoutMs;
		int probeTimeout = (int)Math.min(raftConfig.getAppendEntriesTimeout(), 1000L);
		while (true) {
			if (future.isDone())
				return future.getNow(null);
			netConfig.forEachConnector(c -> {
				if (excludeNames.contains(c.getName()))
					return;
				var socket = c.TryGetReadySocket();
				if (socket == null)
					return;
				var get = new GetLeader();
				get.Send(socket, p -> {
					var g = (GetLeader)p;
					if (!g.isTimeout() && g.getResultCode() == 0) {
						var leaderId = g.Result.getLeaderId();
						if (leaderId != null && !leaderId.isEmpty() && !excludeNames.contains(leaderId)) {
							var found = netConfig.findConnector(leaderId);
							if (found != null)
								future.setResult(found);
						}
					}
					return 0;
				}, probeTimeout);
			});
			long remain = deadline - System.currentTimeMillis();
			if (remain <= 0)
				return future.getNow(null);
			if (future.await(Math.min(remain, 500)))
				return future.getNow(null);
		}
	}

	public java.util.List<ConnectorProxy> getActiveSuggestMajorityConnectors() {
		var result = new ArrayList<ConnectorProxy>();
		for (var node : raftConfig.getNodes().values()) {
			if (!node.isSuggestMajority())
				continue;
			//noinspection DataFlowIssue
			var connector = client.getConfig().findConnector(node.getName());
			if (null != connector && connector.TryGetReadySocket() != null)
				result.add(new ConnectorProxy(connector));
		}
		return result;
	}

	/**
	 * 把Leader赶回主机房（建议节点成为Leader）。
	 *
	 * @param raftConfig raftConfig
	 * @return null，表示成功，！null表示错误描述。
	 */
	public static @Nullable String driveOutNotSuggestMajorityLeader(RaftConfig raftConfig) {
		var agentOut = new OutObject<Agent>();
		var stoppeds = new LinkedHashSet<Connector>();
		var stoppedNames = new HashSet<String>();
		try {
			var leader = waitForLeader(raftConfig, 0, agentOut);
			if (null == leader)
				return "no leader.";
			var leaderNode = raftConfig.getNodes().get(leader.getName());
			if (leaderNode == null)
				return "unknown leader node: " + leader.getName();
			if (leaderNode.isSuggestMajority())
				return null;

			// 检测活跃的建议的节点数量足够，达到多数派；
			//noinspection DataFlowIssue
			if (agentOut.value.getActiveSuggestMajorityConnectors().size() < raftConfig.getMajorityCount())
				return "active suggest majority count not enough.";
			// 逐个停止非建议节点连接以平滑驱赶 Leader，避免同时停止导致最新日志无法传播。
			while (!leaderNode.isSuggestMajority()) {
				var stopServer = new StopServerConnector();
				stopServer.SendForWait(leader.TryGetReadySocket()).await();
				if (stopServer.getResultCode() != 0)
					return "stop server for " + leader.getName() + " error=" + IModule.getErrorCode(stopServer.getResultCode());
				stoppeds.add(leader);
				stoppedNames.add(leader.getName());
				// 等待选举出不在已停止集合内的新 Leader，避免停连接后仍向旧 Leader 忙查。
				leader = waitForLeader(agentOut.value, raftConfig, stoppedNames, 0);
				if (null == leader)
					return "no leader.";
				leaderNode = raftConfig.getNodes().get(leader.getName());
				if (leaderNode == null)
					return "unknown leader node: " + leader.getName();
			}
			return null;
		} catch (Exception ex) {
			return ex.toString();
		} finally {
			// 尽力重启所有已停节点，单节点失败仅记日志，避免中断后续节点恢复与 agent 释放。
			for (var stopped : stoppeds) {
				try {
					var startServer = new StartServerConnector();
					startServer.SendForWait(stopped.TryGetReadySocket()).await();
					if (startServer.getResultCode() != 0)
						logger.error("start server for {} error={}",
								stopped.getName(), IModule.getErrorCode(startServer.getResultCode()));
				} catch (Exception e) {
					logger.error("start server for {} error", stopped.getName(), e);
				}
			}
			// 释放由 waitForLeader 创建并移交的临时 agent。
			if (agentOut.value != null) {
				try {
					agentOut.value.stop();
				} catch (Exception e) {
					logger.error("driveOutNotSuggestMajorityLeader stop agent error.", e);
				}
			}
		}
	}
}
