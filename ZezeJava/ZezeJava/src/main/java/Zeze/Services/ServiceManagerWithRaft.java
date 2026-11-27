package Zeze.Services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.GetLeader;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.Server;
import Zeze.Raft.StartServerConnector;
import Zeze.Raft.StopServerConnector;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.Random;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.DaemonTimer;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 基于Raft共识的服务管理器：多节点部署下提供与ServiceManagerServer一致的服务注册/
 * 发现/负载转发，状态经rocks-raft复制持久化，所有raft侧过程体经SM锁单写者串行执行。
 */
public final class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft implements AutoCloseable {

	private static final Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final Rocks rocks;
	private final DaemonTimer reconcileDaemon = new DaemonTimer("ServiceManagerWithRaft.reconcileSessions", 60_000, this::reconcileSessions);
	// 停机标记：SM 锁内的 Raft 任务体首查此位，置位后直接退出不再访问 rocks
	private volatile boolean closed;
	private final Table<String, BAutoKey> tableAutoKey;
	private final Table<String, BSession> tableSession;
	private final Table<String, BLoadObservers> tableLoadObservers;
	private final Table<String, BServerState> tableServerState;

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf conf = new ServiceManagerServer.Conf();

	public ServiceManagerWithRaft(String raftName, @Nullable RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, Config.load());
	}

	public ServiceManagerWithRaft(String raftName, @Nullable RaftConfig raftConf, @Nullable Config config) throws Exception {
		ZezeCounter.tryInit();
		ServiceManagerServer.applyLogLevelProperty(); // 显式启动动作（仅显式指定logLevel属性才动配置）

		if (config == null)
			config = Config.load();
		config.parseCustomize(conf);

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config,
			SMServer::new, new TaskOneByOneByKey());

		try {
			RegisterRocksTables(rocks);
			//noinspection DataFlowIssue
			RegisterProtocols(rocks.getRaft().getServer());

			//noinspection DataFlowIssue
			tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
			//noinspection DataFlowIssue
			tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
			//noinspection DataFlowIssue
			tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
			//noinspection DataFlowIssue
			tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();
			// 处理器可能在start返回前运行，依赖表必须先全部就绪。
			rocks.getRaft().getServer().start();

			// 会话清理对账：60s粒度足够，快速路径由closeSession的退避重试承担。
			reconcileDaemon.start();
		} catch (Exception e) {
			try {
				rocks.close();
			} catch (Exception closeError) {
				e.addSuppressed(closeError);
			}
			throw e;
		}
	}

	@Override
	public void close() {
		closed = true;
		reconcileDaemon.stop();
		// 先置 closed 再过 SM 锁屏障确保无在飞过程体访问 rocks，锁外关闭 rocks 以免与网络停机互等
		lock();
		unlock();
		// 取消 Takeover 升档定时器（rocks.close 前后各清一次，避免关停期间新插入残留）
		cancelSuspectTiers();
		rocks.close();
		cancelSuspectTiers();
	}

	/** 调用方全程持SM锁；只有已应用完整日志前缀，才能基于当前状态生成下一条增量。 */
	private Procedure newProcedure(FuncLong action) {
		var procedure = rocks.newProcedure(action);
		procedure.setFunc(() -> {
			// Leader 尚未应用完整日志前缀时返回 RaftRetry，防止基于旧缓存生成与迟到日志冲突的增量
			var raft = rocks.getRaft();
			//noinspection DataFlowIssue
			raft.lock();
			try {
				var sequence = raft.getLogSequence();
				if (raft.isLeader() && sequence.getLastApplied() != sequence.getLastIndex())
					return Zeze.Transaction.Procedure.RaftRetry;
				// 准入后若换主，即使本节点随后重新当选，也不能提交按旧任期状态计算的增量。
				procedure.setExpectedTerm(sequence.getTerm());
			} finally {
				raft.unlock();
			}
			return action.call();
		});
		return procedure;
	}

	// Leader 周期对账：在 SM 锁内扫描并清理连接已断但尚未落库清理的残留会话行
	private void reconcileSessions() throws Exception {
		var raft = rocks.getRaft();
		lock();
		try {
			// closed 检查与 rocks 访问同在 SM 锁内，与 close() 锁屏障互斥
			if (closed)
				return;
			//noinspection DataFlowIssue
			if (!raft.isWorkingLeader())
				return;
			var server = raft.getServer();
			record DeadSession(String name, long sessionId) {
			}
			var dead = new ArrayList<DeadSession>();
			tableSession.walk((name, row) -> {
				// 行的sessionId随重连Login更新为新连接，GetSocket判活不会误杀重连后的会话。
				if (server.GetSocket(row.getSessionId()) == null)
					dead.add(new DeadSession(name, row.getSessionId()));
				return true;
			});
			if (dead.isEmpty())
				return;
			var rc = newProcedure(() -> {
				for (var d : dead)
					cleanupSessionRow(d.name(), d.sessionId());
				return 0L;
			}).call();
			if (rc != 0)
				logger.error("reconcileSessions rc={}, dead={}", rc, dead.size());
		} finally {
			unlock();
		}
	}

	// 幂等清理指定会话行（事务内执行）：校验 sessionId 归属后退订、跨版本注销、清理观察者并删行
	private void cleanupSessionRow(String name, long sessionId) {
		var session = tableSession.get(name);
		// 已被同名新连接接管时跳过清理，避免误删新会话状态
		if (null != session && session.getSessionId() != sessionId)
			return;
		if (null != session) {
			for (var info : session.getSubscribes().values())
				unSubscribeNow(name, info.getServiceName());

			// 跨全部版本桶注销，防止跨版本重注册后旧版本桶残留
			var notifies = new HashMap<AsyncSocket, Edit>();
			for (var unReg : session.getRegisters().values()) {
				var state = tableServerState.get(unReg.getServiceName());
				if (state != null)
					removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), name, notifies);
			}
			// remove 通知须在 Raft 提交成功后发出，避免提交失败重跑或回滚导致订阅者视图分叉
			var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
			if (t != null)
				t.runWhileCommit(() -> sendNotifies(notifies));
			else
				sendNotifies(notifies);
		}
		// 联动清理该会话登记的负载观察者并删空行；walk 返回游离拷贝，须先收集 key 再经事务 getOrAdd 修改
		var observedRows = new ArrayList<String>();
		try {
			tableLoadObservers.walk((key, row) -> {
				if (row.getObservers().Contains(name))
					observedRows.add(key);
				return true;
			});
			for (var key : observedRows) {
				var row = tableLoadObservers.getOrAdd(key);
				row.getObservers().remove(name);
				if (row.getObservers().size() == 0)
					tableLoadObservers.remove(key);
			}
		} catch (Exception e) {
			// 回滚本轮清理，保留会话行供closeSession重试及周期对账再次定位。
			throw Zeze.Util.Task.forceThrow(e);
		}
		tableSession.remove(name);
	}

	/**
	 * 所有Raft网络层收到的请求和Rpc的结果，全部加锁，直接运行。
	 * 这样整个程序就单线程化了。
	 */
	public class SMServer extends Server {
		public SMServer(Raft raft, String name, Config config) {
			super(raft, name, config);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																	ProtocolFactoryHandle<?> factoryHandle) {
			lock();
			try {
				if (closed)
					return; // 服务已关闭：响应不再落地（连接随raft停机关闭）
				if (logger.isDebugEnabled())
					logger.debug("dispatchRaftRpcResponse: {}{}", rpc.getClass().getName(), rpc);
				// 完成回调不走业务日志追平门禁，确保 KeepAlive 超时等本地收口事件及时执行
				TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc).call();
			} finally {
				unlock();
			}
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) {
			// 派发到线程池并在任务内持 SM 锁，避免在 IO 线程上等待 Raft appendLog 阻塞心跳与复制
			TaskSpec.ofFunc(() -> {
					lock();
					try {
						if (closed)
							return Zeze.Transaction.Procedure.RaftRetry;
						if (logger.isDebugEnabled()) {
							//noinspection DataFlowIssue
							var netSession = (Session)p.getSender().getUserState();
							var ssName = null != netSession ? netSession.name : "";
							logger.debug("dispatchRaftRequest: {}@{}{}", p.getClass().getName(), ssName, p);
						}
						// 管理协议不涉及 Bean 增量，不经过业务日志追平门禁与 RocksRaft 事务
						if (p instanceof GetLeader || p instanceof StartServerConnector || p instanceof StopServerConnector)
							return TaskSpec.ofFunc(func, p, (__, code) -> p.SendResultCode(code)).call();
						var procedure = newProcedure(func);
						return TaskSpec.ofFunc(procedure::call, p,
								(__, code) -> p.SendResultCode(code)).call();
					} finally {
						unlock();
					}
				}).name(name).onCancel(cancel).dispatchMode(mode)
				.executeOneByOne(((IRaftRpc)p).getUnique(), taskOneByOne);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
			var netSession = (Session)so.getUserState();
			if (null != netSession)
				closeSession(netSession, 0);
			super.OnSocketClose(so, e);
		}

		// 在后台线程池执行幂等会话清理，提交失败时指数退避重试（最终未收敛的残留行由周期对账清理）
		private void closeSession(Session netSession, int retry) {
			// 先于 closed 检查同步取消心跳定时器，防止关库后定时任务访问已关闭的 Raft
			netSession.cancelKeepAlive();
			Raft.executeImportantTask(() -> {
				lock();
				try {
					if (closed)
						return; // 服务已关闭：清理不再落地（重启后由对账收敛），也不再重试
					var rc = newProcedure(() -> {
						netSession.onClose();
						return 0L;
					}).call();
					if (rc == 0)
						return;
					if (retry < 8) {
						logger.error("OnSocketClose session close rc={}, retry {}/8, session={}",
							rc, retry, netSession.name);
						TaskSpec.ofAction(() -> closeSession(netSession, retry + 1))
							.scheduleNow(100L << Math.min(retry, 6));
					} else
						logger.fatal("OnSocketClose session close failed finally, session={}, rc={}",
							netSession.name, rc);
				} catch (Throwable ex) {
					if (retry < 8) {
						logger.error("OnSocketClose session close exception, retry {}/8, session={}",
							retry, netSession.name, ex);
						TaskSpec.ofAction(() -> closeSession(netSession, retry + 1))
							.scheduleNow(100L << Math.min(retry, 6));
					} else
						logger.fatal("OnSocketClose session close failed finally, session={}",
							netSession.name, ex);
				} finally {
					unlock();
				}
			});
		}
	}

	public class Session {
		private final String name;
		private final long sessionId;
		private final @Nullable Future<?> keepAliveTimerTask;
		// Identify上报的serverId；-1=未上报。断线时据此广播Suspect。
		// 仅内存：换leader后agent重新Login+Identify（raftOnSetLeader），无需raft持久化。
		private volatile int identifyServerId = -1;
		private volatile long identifyVersion = -1; // Identify上报的进程数据格式版本；-1=未上报（旧客户端按0）。断线时据此做接管梯队。

		public Session(String name, long sessionId) {
			this.name = name;
			this.sessionId = sessionId;

			if (conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = TaskSpec.ofAction(() -> {
					//noinspection DataFlowIssue
					var s = rocks.getRaft().getServer().GetSocket(sessionId);
					if (s == null)
						return;
					try {
						var r = new KeepAlive();
						// 异步发送并在回调中判活关闭，避免在调度线程同步等待耗尽调度池
						if (!r.Send(s, response -> {
							if (response.isTimeout() || response.getResultCode() != 0)
								s.close(new java.io.IOException("KeepAlive fail: " + response));
							return 0;
						}))
							s.close(new java.io.IOException("KeepAlive send fail"));
					} catch (Throwable ex) {
						s.close(ex);
					}
				}).schedulePeriodNow(
					Random.getInstance().nextInt(conf.keepAlivePeriod),
					conf.keepAlivePeriod);
			} else
				keepAliveTimerTask = null;
		}

		// 取消心跳任务（onClose 及重复 Login 覆盖旧 Session 时均须调用）
		public void cancelKeepAlive() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);
		}

		public void onClose() {
			cancelKeepAlive();

			// 发起 Suspect 梯队广播（仅作加速提示，实际接管由租约表裁决）
			var suspectServerId = identifyServerId;
			if (suspectServerId >= 0) {
				var deadVersion = identifyVersion >= 0 ? identifyVersion : 0; // 未上报version（旧客户端）按0
				startSuspectTier(suspectServerId, sessionId, deadVersion);
			}

			cleanupSessionRow(name, sessionId);
		}
	}

	@Override
	protected long ProcessLoginRequest(Login r) {
		// 已解码排队的 Login 可能晚于断连清理和同名新连接的 Login，不能重新接管新行。
		if (!isSenderAlive(r.getSender()))
			return ErrorNotLogin;
		// 拒绝空白 sessionName，避免多个未配置节点共享同一会话行互相接管
		if (r.Argument.getSessionName().isBlank())
			return ErrorSessionName;
		var session = tableSession.getOrAdd(r.Argument.getSessionName());
		session.setSessionId(r.getSender().getSessionId());
		// 传输会话及其心跳没有事务日志，和成功应答一起在提交后交接。
		var sender = r.getSender();
		var name = r.Argument.getSessionName();
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(() -> completeLogin(r, sender, name));
		else
			completeLogin(r, sender, name);
		return 0;
	}

	private void completeLogin(Login request, AsyncSocket sender, String name) {
		if (!isSenderAlive(sender))
			return;
		var oldSession = sender.getUserState();
		var session = new Session(name, sender.getSessionId());
		sender.setUserState(session);
		if (oldSession instanceof Session old)
			old.cancelKeepAlive();
		// OnSocketClose可能在换userState之前读到旧会话，交接后复查以免遗漏新定时器。
		if (!isSenderAlive(sender)) {
			((SMServer)sender.getService()).closeSession(session, 0);
			return;
		}
		request.SendResult();
	}

	// ProcessAllocateId128Request有意不实现：id128正规路径是UDP（Id128UdpServer），
	// raft版SM不支持id128（ServiceManagerAgentWithRaft构造对History组合fail-fast）。

	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) throws Exception {
		var count = r.Argument.getCount();
		var name = r.Argument.getName();
		// 校验分配数量与 name 长度上限，防止恶意大 count 或超长 key 放大 Raft 持久状态
		//noinspection DataFlowIssue
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX
			|| name.getBytes(StandardCharsets.UTF_8).length > 128) {
			//noinspection DataFlowIssue
			warnAllocateIdRejected("invalid count=" + count + " name.chars=" + name.length());
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		// tAutoKey 存已交付游标不可删行逐出（删行会重号），超唯一名上限直接拒绝
		if (tableAutoKey.get(name) == null) {
			var names = new int[1];
			tableAutoKey.walkKey(k -> {
				names[0]++;
				return names[0] < Id128UdpServer.MAX_UNIQUE_NAMES; // 数够上限即止
			});
			if (names[0] >= Id128UdpServer.MAX_UNIQUE_NAMES) {
				warnAllocateIdRejected("unique names exceeded " + Id128UdpServer.MAX_UNIQUE_NAMES);
				return Zeze.Transaction.Procedure.ErrorRequestId;
			}
		}

		var autoKey = tableAutoKey.getOrAdd(r.Argument.getName());
		r.Result.setStartId(autoKey.getCurrent());
		long current = autoKey.getCurrent() + count;
		autoKey.setCurrent(current);
		r.Result.setCount(count);

		// 须在 Raft 提交成功后应答，避免回滚后重发同一号段
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	// 拒绝告警限频（每 60 秒至多一条）
	private volatile long lastAllocateIdRejectLogMs;

	private void warnAllocateIdRejected(String reason) {
		var now = System.currentTimeMillis();
		var last = lastAllocateIdRejectLogMs;
		if (now - last < 60_000)
			return;
		lastAllocateIdRejectLogMs = now;
		logger.error("AllocateId(raft) rejected (possible attack or misbehaving client): {}", reason);
	}

	// 入口字段长度、单批条数与单会话上限（与 ServiceManagerServer 一致）
	private static final int SVC_NAME_MAX_BYTES = 128;
	private static final int SVC_IDENTITY_MAX_BYTES = 128;
	private static final int SVC_IP_MAX_BYTES = 64;
	private static final int SVC_EXTRA_MAX_BYTES = 256;
	private static final int SVC_EDIT_BATCH_MAX = 128;
	private static final int SVC_PER_SESSION_MAX = 64;

	private static boolean isOverUtf8Bytes(@Nullable String s, int maxBytes) {
		return s != null && s.getBytes(StandardCharsets.UTF_8).length > maxBytes;
	}

	private volatile long lastSvcRejectLogMs;

	private void warnSvcRejected(String reason) {
		var now = System.currentTimeMillis();
		var last = lastSvcRejectLogMs;
		if (now - last < 60_000)
			return;
		lastSvcRejectLogMs = now;
		logger.error("SM(raft) edit/subscribe rejected (possible attack or misbehaving client): {}", reason);
	}

	/**
	 * 检查全局唯一 serviceName 上限：满员时先清理无注册无订阅的空壳行腾位，无空壳可清则拒绝。
	 */
	private boolean checkUniqueServiceName(String name) {
		var plan = planUniqueServiceNames(Set.of(name), Set.of(), "");
		if (plan == null)
			return false;
		for (var key : plan)
			tableServerState.remove(key);
		return true;
	}

	private @Nullable ArrayList<String> planUniqueServiceNames(Set<String> requested, Set<BServiceInfoKeyRocks> removes, String sessionName) {
		int needed = 0;
		for (var name : requested) {
			if (tableServerState.get(name) == null)
				++needed;
		}
		var count = new int[1];
		var idleKeys = new ArrayList<String>();
		try {
			tableServerState.walk((key, row) -> {
				++count[0];
				if (!requested.contains(key) && row.getSimple().size() == 0) {
					boolean idle = true;
					for (var bucket : row.getServiceInfosVersion().values()) {
						for (var info : bucket.getServiceInfos().values()) {
							if (!info.getSessionName().equals(sessionName)
									|| !removes.contains(new BServiceInfoKeyRocks(info.getServiceName(), info.getServiceIdentity()))) {
								idle = false;
								break;
							}
						}
						if (!idle)
							break;
					}
					if (idle)
						idleKeys.add(key);
				}
				return true;
			});
		} catch (Exception e) {
			throw Zeze.Util.Task.forceThrow(e);
		}
		int deficit = count[0] + needed - Id128UdpServer.MAX_UNIQUE_NAMES;
		if (deficit > idleKeys.size()) {
			warnSvcRejected("unique service names exceeded " + Id128UdpServer.MAX_UNIQUE_NAMES);
			return null;
		}
		return new ArrayList<>(idleKeys.subList(0, Math.max(deficit, 0)));
	}

	// 仅更新内存 Session 字段，不写 Raft 表；换主后客户端重连会自动重发 Identify
	@Override
	protected long ProcessIdentifyRequest(Identify r) {
		//noinspection DataFlowIssue
		var netSession = (Session)r.getSender().getUserState();
		if (netSession != null) {
			netSession.identifyServerId = r.Argument.serverId;
			netSession.identifyVersion = r.Argument.version;
			logger.info("{}: Identify serverId={} version={}", r.getSender(), r.Argument.serverId, r.Argument.version);
		}
		r.SendResult();
		return 0;
	}

	// Takeover 版本梯队通知（语义同 ServiceManagerServer）：档间超时默认 900s，状态仅驻留内存，切主/重启由 GS 扫描兜底
	public volatile long takeoverNotifyStepMs = 900_000;

	private static final class SuspectTier {
		final long deadVersion;
		volatile long tierVersion;
		volatile @Nullable Future<?> timer;
		SuspectTier(long deadVersion, long tierVersion) {
			this.deadVersion = deadVersion;
			this.tierVersion = tierVersion;
		}
	}

	private final java.util.concurrent.ConcurrentHashMap<Integer, SuspectTier> suspectTiers = new java.util.concurrent.ConcurrentHashMap<>();

	/** 梯队广播一档：只发 identifyVersion==tierVersion 的存活会话（死者会话与未Identify排除）。 */
	private void broadcastSuspectTier(int deadServerId, long deadSessionId, long deadVersion, long tierVersion) {
		var raft = rocks.getRaft();
		if (closed || raft == null)
			return;
		var suspect = new Suspect();
		suspect.Argument.serverId = deadServerId;
		suspect.Argument.version = deadVersion;
		try {
			raft.getServer().foreach(so -> {
				if (so.getSessionId() == deadSessionId)
					return; // 刚断线的会话本身不报信
				var netSession = (Session)so.getUserState();
				if (netSession == null)
					return; // raft节点间连接没有Login过
				if (netSession.identifyServerId < 0 || netSession.identifyVersion != tierVersion)
					return; // 梯队过滤（需求①③）
				so.Send(suspect);
			});
		} catch (Exception e) {
			logger.warn("Suspect tier broadcast for serverId={} tier={} failed", deadServerId, tierVersion, e);
		}
	}

	private void cancelSuspectTiers() {
		for (var tier : suspectTiers.values()) {
			var timer = tier.timer;
			if (timer != null)
				timer.cancel(false);
		}
		suspectTiers.clear();
	}

	/** onClose入口：广播首档（==死者version）并安排升档链。 */
	private void startSuspectTier(int deadServerId, long deadSessionId, long deadVersion) {
		if (closed)
			return;
		var tier = new SuspectTier(deadVersion, deadVersion);
		var old = suspectTiers.put(deadServerId, tier);
		if (old != null && old.timer != null)
			//noinspection DataFlowIssue
			old.timer.cancel(false); // 死者反复闪断：新周期覆盖旧升档链
		if (closed) {
			suspectTiers.remove(deadServerId, tier);
			return;
		}
		broadcastSuspectTier(deadServerId, deadSessionId, deadVersion, deadVersion);
		scheduleNextTier(deadServerId, deadSessionId, tier);
	}

	/** 档间超时：无回执升到"更高存活version的最小值"；没有更高档=放弃（scan兜底）。 */
	private void scheduleNextTier(int deadServerId, long deadSessionId, SuspectTier tier) {
		if (closed)
			return;
		tier.timer = TaskSpec.ofAction(() -> {
			if (closed)
				return;
			var cur = suspectTiers.get(deadServerId);
			if (cur != tier)
				return; // 已被回执移除/被新周期覆盖
			var raft = rocks.getRaft();
			if (raft == null)
				return;
			var next = new long[]{Long.MAX_VALUE};
			final var deadSessionIdRef = deadSessionId;
			try {
				raft.getServer().foreach(so -> {
					if (so.getSessionId() == deadSessionIdRef)
						return;
					var netSession = (Session)so.getUserState();
					if (netSession == null || netSession.identifyServerId < 0)
						return;
					var v = netSession.identifyVersion;
					if (v > cur.tierVersion && v < next[0])
						next[0] = v; // 下一档=更高存活version的最小值（需求②升序）
				});
			} catch (Exception e) {
				logger.warn("Suspect tier scan for serverId={} failed", deadServerId, e);
			}
			if (closed)
				return;
			if (next[0] == Long.MAX_VALUE) {
				suspectTiers.remove(deadServerId, cur); // 没有更高档：放弃升档
				logger.info("Suspect tier exhausted for serverId={} lastTier={}", deadServerId, cur.tierVersion);
				return;
			}
			cur.tierVersion = next[0];
			broadcastSuspectTier(deadServerId, deadSessionIdRef, cur.deadVersion, next[0]);
			scheduleNextTier(deadServerId, deadSessionIdRef, cur);
		}).scheduleNow(takeoverNotifyStepMs);
		if (closed) {
			var timer = tier.timer;
			if (timer != null)
				timer.cancel(false);
			suspectTiers.remove(deadServerId, tier);
		}
	}

	@Override
	protected long ProcessTakeoverDoneRequest(TakeoverDone r) {
		var tier = suspectTiers.remove(r.Argument.deadServerId);
		if (tier != null && tier.timer != null)
			//noinspection DataFlowIssue
			tier.timer.cancel(false);
		logger.info("TakeoverDone serverId={} taker={} version={} tier={}",
				r.Argument.deadServerId, r.Argument.takerServerId, r.Argument.takerVersion,
				tier == null ? -1 : tier.tierVersion);
		r.SendResult();
		return 0;
	}

	private void addLoadObserver(String ip, int port, String sessionName) {
		if (!ip.isEmpty() && port != 0) {
			var loadObservers = tableLoadObservers.getOrAdd(ip + "_" + port);
			loadObservers.getObservers().add(sessionName);
		}
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) {
		// 仅向已登记观察者的地址转发，无观察者行时直接忽略不建新行
		if (isOverUtf8Bytes(r.Argument.ip, SVC_IP_MAX_BYTES)) {
			warnSvcRejected("setLoad ip over size");
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		var netSession = requireSession(r);
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var loadObservers = tableLoadObservers.get(r.Argument.ip + "_" + r.Argument.port);
		if (loadObservers == null) {
			r.SendResult(); // 无状态修改也须确认 RPC，避免客户端超时重发。
			return 0; // 无观察者：忽略上报，不建行
		}
		var observers = loadObservers.getObservers();

		var set = new SetServerLoad();
		set.Argument = r.Argument;

		ArrayList<String> removed = null;
		for (var observer : observers) {
			try {
				var session = tableSession.get(observer);
				//noinspection DataFlowIssue
				if (null != session && set.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId())))
					continue;
			} catch (Throwable ignored) {
			}
			if (removed == null)
				removed = new ArrayList<>();
			removed.add(observer);
		}
		if (removed != null) {
			for (var remove : removed)
				observers.remove(remove);
		}
		// 死观察者移除依赖 Raft 提交，应答延迟到提交后发出；负载转发直接推送即可
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	private static BServiceInfoRocks toRocks(BServiceInfo serverInfo, String sessionName) {
		return new BServiceInfoRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity(),
			serverInfo.getPassiveIp(), serverInfo.getPassivePort(), serverInfo.getExtraInfo(),
			sessionName, serverInfo.getVersion());
	}

	// 校验 identity 格式（非 '@'/'#' 前缀须可解析为 long），防止非法值破坏订阅端排序
	private static boolean isLegalServiceIdentity(String identity) {
		if (identity.startsWith("@") || identity.startsWith("#"))
			return true;
		try {
			Long.parseLong(identity);
			return true;
		} catch (NumberFormatException e) {
			logger.warn("illegal service identity: '{}'", identity);
			return false;
		}
	}

	private static BServiceInfoKeyRocks toRocksKey(BServiceInfo serverInfo) {
		return new BServiceInfoKeyRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity());
	}

	private static BSubscribeInfoRocks toRocks(BSubscribeInfo si) {
		return new BSubscribeInfoRocks(si.getServiceName(), si.getVersion());
	}

	private static void sendNotifies(HashMap<AsyncSocket, Edit> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	// 未Login会话统一拒绝码：Procedure保留码用到-17，本模块局部取-18。
	public static final long ErrorNotLogin = -18;

	// 空白sessionName拒绝码：Procedure保留码现已占到-20（Busy=-19、AuthFail=-20），
	// 本模块局部取-21。
	public static final long ErrorSessionName = -21;

	/**
	 * 会话名前置条件还必须验证连接归属：同名新连接 Login 后旧连接仍有 Session，
	 * 但它不能再修改新连接的注册/订阅。与 cleanupSessionRow 使用同一持久 sessionId 契约。
	 */
	private @Nullable Session requireSession(Zeze.Net.Rpc<?, ?> r) {
		var sender = r.getSender();
		if (sender == null || !isSenderAlive(sender) || !(sender.getUserState() instanceof Session netSession))
			return notLogin(r);
		var row = tableSession.get(netSession.name);
		return row != null && row.getSessionId() == sender.getSessionId() ? netSession : notLogin(r);
	}

	private boolean isSenderAlive(@Nullable AsyncSocket sender) {
		// 调用者持 SM 锁且已经通过 closed 门禁；close 的锁屏障之前 Raft 仍然有效。
		//noinspection DataFlowIssue
		return sender != null && rocks.getRaft().getServer().GetSocket(sender.getSessionId()) == sender;
	}

	private @Nullable Session notLogin(Zeze.Net.Rpc<?, ?> r) {
		r.SendResultCode(ErrorNotLogin);
		return null;
	}

	@Override
	@SuppressWarnings("ConstantConditions") // 保留非空 extraInfo 契约之外的防御判断。
	protected long ProcessEditRequest(Edit r) {
		for (var info : r.Argument.getAdd())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Zeze.Transaction.Procedure.ErrorRequestId;
		for (var info : r.Argument.getRemove())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Zeze.Transaction.Procedure.ErrorRequestId;
		var netSession = requireSession(r);
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var notifies = new HashMap<AsyncSocket, Edit>();

		// 校验批大小、字段长度、单会话上限与全局唯一名上限（满员时事务内清理空壳行腾位）
		if (r.Argument.getAdd().size() + r.Argument.getRemove().size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("edit batch exceeded " + SVC_EDIT_BATCH_MAX);
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		for (var info : r.Argument.getAdd()) {
			if (isOverUtf8Bytes(info.getServiceName(), SVC_NAME_MAX_BYTES)
				|| isOverUtf8Bytes(info.getServiceIdentity(), SVC_IDENTITY_MAX_BYTES)
				|| isOverUtf8Bytes(info.getPassiveIp(), SVC_IP_MAX_BYTES)
				|| (info.getExtraInfo() != null && info.getExtraInfo().size() > SVC_EXTRA_MAX_BYTES)) {
				warnSvcRejected("edit field over size: " + info.getServiceName());
				return Zeze.Transaction.Procedure.ErrorRequestId;
			}
		}
		var finalAdds = ServiceManagerServer.finalRegistrations(r.Argument.getAdd());
		var sessionRow = tableSession.get(netSession.name);
		var finalRegisters = new HashSet<>(sessionRow.getRegisters().keys());
		var removeKeys = new HashSet<BServiceInfoKeyRocks>();
		for (var info : r.Argument.getRemove()) {
			var key = toRocksKey(info);
			finalRegisters.remove(key);
			removeKeys.add(key);
		}
		var requestedNames = new HashSet<String>();
		for (var info : finalAdds) {
			finalRegisters.add(toRocksKey(info));
			requestedNames.add(info.getServiceName());
		}
		if (finalRegisters.size() > SVC_PER_SESSION_MAX) {
			warnSvcRejected("session registers exceeded " + SVC_PER_SESSION_MAX);
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		var evictions = planUniqueServiceNames(requestedNames, removeKeys, netSession.name);
		if (evictions == null)
			return Zeze.Transaction.Procedure.ErrorRequestId;

		// step 1: remove
		for (var unReg : r.Argument.getRemove()) {
			var state = tableServerState.get(unReg.getServiceName());
			if (state != null)
				removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), netSession.name, notifies);
			var session = tableSession.get(netSession.name);
			//noinspection DataFlowIssue
			session.getRegisters().remove(toRocksKey(unReg)); // ignore remove failed
		}

		for (var name : evictions)
			tableServerState.remove(name);

		// step 2: add
		for (var reg : finalAdds) {
			var session = tableSession.get(netSession.name);
			// 允许重复登录，断线重连Agent不好原子实现重发。
			//noinspection DataFlowIssue
			session.getRegisters().put(toRocksKey(reg), toRocks(reg, netSession.name));
			var state = tableServerState.getOrAdd(reg.getServiceName());
			if (!state.getServiceName().equals(reg.getServiceName()))
				state.setServiceName(reg.getServiceName());
			addAndCollectNotify(state, reg, netSession.name, notifies);
		}

		// 应答与增量通知须在 Raft 提交成功后发出，避免回滚后客户端与订阅者视图不一致
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null) {
			t.runWhileCommit(() -> {
				sendNotifies(notifies);
				r.SendResult();
			});
		} else {
			sendNotifies(notifies);
			r.SendResult();
		}
		return 0;
	}

	private void addAndCollectNotify(BServerState state, BServiceInfo info, String sessionName,
									 HashMap<AsyncSocket, Edit> notifies) {
		// AddOrUpdate 语义：同 identity 跨版本重注册时先从其他版本桶移除旧记录并通知其订阅者
		for (var e : state.getServiceInfosVersion().entrySet()) {
			if (e.getKey() == info.getVersion())
				continue;
			// CollMap2.remove 返回 void，须先取旧值再移除以发送 remove 通知
			var old = e.getValue().getServiceInfos().get(info.getServiceIdentity());
			if (old != null) {
				e.getValue().getServiceInfos().remove(info.getServiceIdentity());
				collectNotify(state, fromRocks(old), false, notifies);
			}
		}
		pruneEmptyVersions(state);
		var versions = state.getServiceInfosVersion().get(info.getVersion());
		if (null == versions)
			state.getServiceInfosVersion().put(info.getVersion(), versions = new BServiceInfosVersionRocks());
		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		versions.getServiceInfos().put(info.getServiceIdentity(), toRocks(info, sessionName));
		collectNotify(state, info, true, notifies);
		// 为现有订阅者补登新注册实例的负载观察者
		for (var observer : state.getSimple().keys())
			addLoadObserver(info.getPassiveIp(), info.getPassivePort(), observer);
	}

	// 通知订阅了info版本的会话（version==0订阅全部版本）。info的版本决定通知过滤。
	private void collectNotify(BServerState state, BServiceInfo info, boolean isAdd,
							   HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getSimple().entrySet()) {
			var subVersion = e.getValue().getVersion();
			if (subVersion == 0 || subVersion == info.getVersion()) {
				var sessionName = e.getKey();
				var session = tableSession.get(sessionName);
				if (null == session)
					continue;
				//noinspection DataFlowIssue
				var peer = rocks.getRaft().getServer().GetSocket(session.getSessionId());
				if (null == peer)
					continue;

				var notify = notifies.computeIfAbsent(peer, __ -> new Edit());
				if (isAdd)
					notify.Argument.getAdd().add(info);
				else
					notify.Argument.getRemove().add(info);
			}
		}
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		var netSession = requireSession(r);
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		// 每会话订阅数+serviceName长度+全局唯一名上限（对齐Edit面）。
		if (r.Argument.subs.size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("subscribe batch exceeded " + SVC_EDIT_BATCH_MAX);
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		//noinspection DataFlowIssue
		var finalSubscriptions = new HashSet<>(tableSession.get(netSession.name).getSubscribes().keys());
		var requestedNames = new HashSet<String>();
		for (var info : r.Argument.subs) {
			if (isOverUtf8Bytes(info.getServiceName(), SVC_NAME_MAX_BYTES)) {
				warnSvcRejected("subscribe name over size");
				return Zeze.Transaction.Procedure.ErrorRequestId;
			}
			finalSubscriptions.add(info.getServiceName());
			requestedNames.add(info.getServiceName());
		}
		if (finalSubscriptions.size() > SVC_PER_SESSION_MAX) {
			warnSvcRejected("session subscribes exceeded " + SVC_PER_SESSION_MAX);
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		var evictions = planUniqueServiceNames(requestedNames, Set.of(), netSession.name);
		if (evictions == null)
			return Zeze.Transaction.Procedure.ErrorRequestId;
		for (var name : evictions)
			tableServerState.remove(name);
		var session = tableSession.get(netSession.name);
		for (var info : r.Argument.subs) {
			//noinspection DataFlowIssue
			session.getSubscribes().put(info.getServiceName(), toRocks(info));
			var state = tableServerState.getOrAdd(info.getServiceName());
			if (!state.getServiceName().equals(info.getServiceName()))
				state.setServiceName(info.getServiceName());
			subscribeAndCollect(state, r, info, netSession.name);
		}
		// 须在 Raft 提交成功后应答，避免回滚后客户端误以为订阅已生效
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	private static BServiceInfo fromRocks(BServiceInfoRocks rocks) {
		return new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
			rocks.getVersion(),
			rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo());
	}

	public void removeAndCollectNotify(BServerState state, BServiceInfo info, HashMap<AsyncSocket, Edit> notifies) {
		collectNotify(state, info, false, notifies);
	}

	// 跨全部版本桶注销属于本会话的记录，归属不符时跳过以免误删新会话的注册
	private void removeAndCollectNotifyAllVersions(BServerState state, String serviceIdentity, String sessionName,
												   HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getServiceInfosVersion().entrySet()) {
			var exist = e.getValue().getServiceInfos().get(serviceIdentity);
			// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
			if (exist == null || !exist.getSessionName().equals(sessionName))
				continue;
			e.getValue().getServiceInfos().remove(serviceIdentity);
			removeAndCollectNotify(state, fromRocks(exist), notifies);
		}
		pruneEmptyVersions(state);
	}

	private static void pruneEmptyVersions(BServerState state) {
		var empty = new ArrayList<Long>();
		for (var entry : state.getServiceInfosVersion().entrySet()) {
			if (entry.getValue().getServiceInfos().size() == 0)
				empty.add(entry.getKey());
		}
		for (var version : empty)
			state.getServiceInfosVersion().remove(version);
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var netSession = requireSession(r);
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var session = tableSession.get(netSession.name);
		for (var serviceName : r.Argument.serviceNames) {
			//noinspection DataFlowIssue
			var sub = session.getSubscribes().get(serviceName);
			session.getSubscribes().remove(serviceName);
			if (sub != null) {
				unSubscribeNow(netSession.name, serviceName);
			}
		}
		// 须在 Raft 提交成功后应答，避免回滚后客户端误以为退订已生效
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	public @Nullable BServerState unSubscribeNow(String sessionName, String serviceName) {
		var state = tableServerState.get(serviceName);
		if (state != null) {
			var removed = state.getSimple().get(sessionName);
			state.getSimple().remove(sessionName);
			if (removed != null)
				return state;
		}
		return null;
	}

	private void subscribeAndCollect(BServerState state, Subscribe r, BSubscribeInfo subInfo, String ssName) {
		// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
		state.getSimple().put(ssName, toRocks(subInfo));
		r.Result.map.put(state.getServiceName(), new BServiceInfosVersion(subInfo.getVersion(), state));

		//noinspection DataFlowIssue
		var netSession = (Session)r.getSender().getUserState();
		for (var versions : state.getServiceInfosVersion().values())
			for (var info : versions.getServiceInfos().values())
				//noinspection DataFlowIssue
				addLoadObserver(info.getPassiveIp(), info.getPassivePort(), netSession.name);
	}
}
