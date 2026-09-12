package Zeze.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;

import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.Server;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.Random;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft implements AutoCloseable {
	static {
		// 【FND4-64】原此处还有root logger级别重置（未设logLevel属性时也强制INFO）——类加载即
		// 篡改全JVM日志配置，已移至构造器的显式启动动作（applyLogLevelProperty，仅显式指定才动）。
		// 补注册 tId128.current（Zeze.Util.Id128，非内置类型）的修改日志工厂：AbstractServiceManagerWithRaft.
		// RegisterRocksTables 漏了 Log1.LogBeanKey<Zeze.Util.Id128>（typeId=1751213859）。
		// 首次 getOrAdd(tId128) 走 Record.Put（整 bean 编码，不需要 Log 工厂）能提交成功；
		// 第二次起修改已存在行走 Record.Edit，日志写入 WAL 时 encode 不需要工厂，但之后任何
		// readLog 重读该日志（AppendEntries 复制、apply、重启恢复）都要 Log.create(1751213859)
		// 抛 UnsupportedOperationException，导致后续所有事务 appendLog→RaftRetry→回滚，
		// 复制通道卡死（实测 UnSubscribe 应答后订阅未删除即此因）。AbstractGlobalCacheManagerWithRaft
		// 对 LogSet1<Integer> 是在自己的静态块补注册的，这里对齐该做法。
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Util.Id128.class));
	}

	private static final @NotNull Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final @NotNull Rocks rocks;
	// 会话清理对账周期任务（FND4-57兜底层），close时取消。
	private final Future<?> reconcileFuture;
	private final @NotNull Table<String, BAutoKey> tableAutoKey;
	private final @NotNull Table<String, BId128> tableId128;
	private final @NotNull Table<String, BSession> tableSession;
	private final @NotNull Table<String, BLoadObservers> tableLoadObservers;
	private final @NotNull Table<String, BServerState> tableServerState;

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf conf = new ServiceManagerServer.Conf();

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, Config.load(), false);
	}

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Config config,
								  boolean RocksDbWriteOptionSync) throws Exception {
		ZezeCounter.tryInit();
		ServiceManagerServer.applyLogLevelProperty(); // FND4-64：显式启动动作（仅显式指定logLevel属性才动配置）

		if (config == null)
			config = Config.load();
		config.parseCustomize(conf);

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync,
			SMServer::new, new TaskOneByOneByKey());

		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().start();

		tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
		tableId128 = rocks.<String, BId128>getTableTemplate("tId128").openTable();
		tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
		tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
		tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();

		// 会话清理对账（FND4-57兜底层）：60s粒度足够，快速路径由closeSession的退避重试承担。
		reconcileFuture = TaskSpec.ofAction(this::reconcileSessions).schedulePeriodNow(60_000, 60_000);
	}

	@Override
	public void close() {
		if (reconcileFuture != null)
			reconcileFuture.cancel(false);
		rocks.close();
	}

	// leader周期对账（FND4-57兜底层）：清理"连接已死但清理事务未落地"的残留会话行。
	// closeSession的退避重试只覆盖"断连节点恢复多数派/重新当选"的场景；leader切换后，
	// 断连风暴的清理在旧leader上永远RaftRetry，进程崩溃则内存待办全丢——对账在现任
	// leader上周期收敛这两类残余（死agent的注册/订阅行、订阅方的幽灵地址）。
	// SM锁内执行，与Login/OnSocketClose串行；walk快照与清理事务之间行被新连接接管时，
	// cleanupSessionRow的归属校验兜底，不会误清。对账事务失败仅记error，下轮周期自带重试。
	private void reconcileSessions() throws Exception {
		var raft = rocks.getRaft();
		if (!raft.isWorkingLeader())
			return;
		lock();
		try {
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
			var rc = rocks.newProcedure(() -> {
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

	// 行清理（raft事务内执行，幂等）：退订/注销跨版本桶/发remove通知/删行。
	// 行不存在或已被新连接接管（sessionId不匹配）时空转。
	// 两个入口：Session.onClose（刚断连，随SMServer.closeSession退避重试落地）与
	// reconcileSessions周期对账（死连接残留行的最终收敛）。
	private void cleanupSessionRow(String name, long sessionId) {
		var session = tableSession.get(name);
		// 清理的执行可能晚于同名新连接的Login（keepalive超时、对账竞态等），此时行已被
		// 新连接接管，校验归属后跳过清理，否则新连接的注册/订阅被误清并删行，其后续请求NPE。
		if (null != session && session.getSessionId() != sessionId)
			return;
		if (null != session) {
			for (var info : session.getSubscribes().values())
				unSubscribeNow(name, info.getServiceName());

			// 注销跨全部版本桶（会话registers以name+id为key只保留最后一次注册，
			// 若按unReg.getVersion()单桶删，跨版本重注册后旧版本桶残留幽灵地址）。
			var notifies = new HashMap<AsyncSocket, Edit>();
			for (var unReg : session.getRegisters().values()) {
				var state = tableServerState.get(unReg.getServiceName());
				if (state != null)
					removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), name, notifies);
			}
			ServiceManagerWithRaft.sendNotifies(notifies);
		}
		// FND4-66：会话关闭联动清理该会话登记的负载观察者——原来仅setLoad转发失败时惰性剔除，
		// 停止上报的地址行（raft持久表）与死观察者永久残留（无界增长）。observers清空即删地址行；
		// walk回调内直接remove有遍历器失效风险，先收集后删。
		var deadAddressRows = new ArrayList<String>();
		try {
			tableLoadObservers.walk((key, row) -> {
				row.getObservers().remove(name);
				if (row.getObservers().size() == 0)
					deadAddressRows.add(key);
				return true;
			});
		} catch (Exception e) { // logger.error
			logger.error("cleanup loadObservers for session {} failed", name, e);
		}
		for (var key : deadAddressRows)
			tableLoadObservers.remove(key);
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
				if (logger.isDebugEnabled())
					logger.debug("dispatchRaftRpcResponse: {}{}", rpc.getClass().getName(), rpc);
				var procedure = rocks.newProcedure(() -> responseHandle.handle(rpc));
				TaskSpec.ofFunc(procedure::call, rpc).call();
			} finally {
				unlock();
			}
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) {
			// 不能在调用线程（Selector IO 线程）上内联执行：raft 提交的 appendLog 等待期间
			// 该 IO 线程被冻结，心跳与 AppendEntries 应答处理停摆，负载/抖动下引发选主动摇；
			// 且 TaskCompletionSource.get 对 Selector 线程有防御断言（测试 JVM 默认 -ea），
			// 等待即 AssertionError 被包装成 RaftRetry(-15)。按传入的 mode 派发到线程池执行
			// （对齐 Raft.Server 基类实现），SM 锁移入任务内，保持单写者串行语义不变。
			TaskSpec.ofFunc(() -> {
					lock();
					try {
						if (logger.isDebugEnabled()) {
							var netSession = (Session)p.getSender().getUserState();
							var ssName = null != netSession ? netSession.name : "";
							logger.debug("dispatchRaftRequest: {}@{}{}", p.getClass().getName(), ssName, p);
						}
						var procedure = new Procedure(rocks, func);
						return TaskSpec.ofFunc(procedure::call, p, Protocol::SendResultCode).call();
					} finally {
						unlock();
					}
				}).name(name).onCancel(cancel).dispatchMode(mode)
				.executeOneByOne(((IRaftRpc)p).getUnique(), taskOneByOne);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, Throwable e) throws Exception {
			var netSession = (Session)so.getUserState();
			if (null != netSession)
				closeSession(netSession, 0);
			super.OnSocketClose(so, e);
		}

		// 会话清理的raft提交结果必须闭环（FND4-57）：RaftRetry是返回码不是异常
		// （RocksRaft Transaction.perform捕获RaftRetryException后返回Procedure.RaftRetry），
		// 曾经的procedure.call()返回码被忽略——leader切换既造成agent断连风暴（旧leader上
		// OnSocketClose批量触发）又恰使旧leader的appendLog失败，回滚后的清理
		// （注销/退订/删行/发remove通知）不重试不告警，死agent的注册与订阅在raft表中
		// 永久残留，订阅方持有幽灵地址持续分发。onClose幂等（归属校验后清理、
		// 不存在即no-op、Suspect为提示性重发），失败退避重试，上限后fatal留观测。
		// 残余缺口=进程崩溃窗口内的清理丢失（无持久化待办），周期对账兜底另立项。
		private void closeSession(Session netSession, int retry) {
			// 同 dispatchRaftRequest：清理的raft提交不能在 IO 线程上等待。
			Raft.executeImportantTask(() -> {
				lock();
				try {
					var rc = rocks.newProcedure(() -> {
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

	/*
	private static BSubscribeInfo fromRocks(BSubscribeInfoRocks rocks) {
		return new BSubscribeInfo(rocks.getServiceName(), rocks.getVersion());
	}
	*/

	public class Session {
		private final String name;
		private final long sessionId;
		private final Future<?> keepAliveTimerTask;
		// Identify上报的serverId；-1=未上报。断线时据此广播Suspect。
		// 仅内存：换leader后agent重新Login+Identify（raftOnSetLeader），无需raft持久化。
		private volatile int identifyServerId = -1;

		public Session(String name, long sessionId) {
			this.name = name;
			this.sessionId = sessionId;

			if (conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = TaskSpec.ofAction(() -> {
					var s = rocks.getRaft().getServer().GetSocket(sessionId);
					// socket已不存在（会话断开，GetSocket返回null）：正常断开态而非错误，
					// 无从发送也无需关闭，会话清理由OnSocketClose负责；曾经null穿透到
					// Send失败分支的sock.close直接NPE，被外层catch吞成周期性error日志。
					if (s == null)
						return;
					try {
						var r = new KeepAlive();
						// 异步等待应答（FND-S1-10，对齐非raft版）：SendAndWaitCheckResultCode在
						// 调度池线程上同步阻塞，半开连接堆积时可耗尽调度池拖停全部周期任务。
						// 回调判活：超时/失败码在回调中关闭连接触发重连。
						if (!r.Send(s, response -> {
							if (response.isTimeout() || response.getResultCode() != 0)
								s.close(new java.io.IOException("KeepAlive fail: " + response));
							return 0;
						}))
							s.close(new java.io.IOException("KeepAlive send fail"));
					} catch (Throwable ex) { // logger.error
						s.close(ex);
					}
				}).schedulePeriodNow(
					Random.getInstance().nextInt(conf.keepAlivePeriod),
					conf.keepAlivePeriod);
			} else
				keepAliveTimerTask = null;
		}

		// 取消keepAlive周期任务。除onClose外，重复Login覆盖socket的userState前也必须调用
		// （FND2-S1-3）：被覆盖的旧Session的onClose永不执行，其定时器永不取消。
		public void cancelKeepAlive() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);
		}

		public void onClose() {
			cancelKeepAlive();

			// Suspect广播：立即、不延迟、不挑选目标（对齐非raft版）。仅是提示（hint），
			// 接收方转化为takeover.tryTransfer，由租约表裁决；未过期租约会被安排到过期时刻精确重试。
			// 短暂掉线误判不存在：接管前租约必须过期，死者重启会claim新epoch。
			var suspectServerId = identifyServerId;
			if (suspectServerId >= 0) {
				try {
					rocks.getRaft().getServer().foreach(so -> {
						if (so.getSessionId() == sessionId)
							return; // 刚断线的会话本身不报信
						var netSession = (Session)so.getUserState();
						if (netSession == null)
							return; // raft节点间连接没有Login过
						var suspect = new Suspect();
						suspect.Argument.serverId = suspectServerId;
						so.Send(suspect);
					});
				} catch (Exception e) {
					logger.warn("Suspect broadcast for serverId={} failed", suspectServerId, e);
				}
			}

			cleanupSessionRow(name, sessionId);
		}
	}

	@Override
	protected long ProcessLoginRequest(Login r) {
		var session = tableSession.getOrAdd(r.Argument.getSessionName());
		// 重复Login（raftOnSetLeader超时递归重发等）在同一socket上覆盖userState前，先取消旧Session
		// 的keepAliveTimerTask（FND2-S1-3）：OnSocketClose只回调最后userState的onClose，被覆盖的
		// Session定时器永不取消——socket关闭后GetSocket(sessionId)恒null，周期性error日志，永不停止。
		var oldSession = r.getSender().getUserState();
		if (oldSession instanceof Session old)
			old.cancelKeepAlive();
		r.getSender().setUserState(new Session(r.Argument.getSessionName(), r.getSender().getSessionId()));
		session.setSessionId(r.getSender().getSessionId());
		// 应答必须raft提交成功后发出（对齐ProcessAllocateIdRequest的修复2eee0da1d、
		// ProcessEditRequest的修复47ec96e18）：tSession的getOrAdd/setSessionId依赖raft提交，
		// 提交前应答在复制失败回滚后客户端已拿到成功码（假成功），其后续Edit/Subscribe读到
		// 回滚的会话状态（tableSession无行NPE转错误码）。非事务上下文（不应发生）保持立即应答。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateId128Request(AllocateId128 r) {
		// 随便写写! 这个实际上没用,因为id128需要通过udp,这里是tcp.
		if (r.Argument.getCount() < 1)
			return Zeze.Transaction.Procedure.ErrorRequestId;

		var id128 = tableId128.getOrAdd(r.Argument.getName());
		r.Result.setStartId(id128.getCurrent());
		var count = r.Argument.getCount();
		id128.setCurrent(id128.getCurrent().add(count)); // 不能直接修改当前值,因为没有受事务保护.
		r.Result.setCount(count);
		// 号段必须raft提交成功后再应答（对齐ProcessAllocateIdRequest的修复2eee0da1d）：
		// appendLog失败回滚current，提交前应答会让客户端把已回滚的号段投入使用，重复发放。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) {
		if (r.Argument.getCount() < 1)
			return Zeze.Transaction.Procedure.ErrorRequestId;

		var autoKey = tableAutoKey.getOrAdd(r.Argument.getName());
		r.Result.setStartId(autoKey.getCurrent());
		// 随便修正一下分配数量。
		var count = r.Argument.getCount();
		long current = autoKey.getCurrent() + count;
		autoKey.setCurrent(current);
		r.Result.setCount(count);

		// 号段必须raft提交成功后再应答：appendLog失败（失主RaftRetry/复制异常）会回滚current，
		// 提交前应答会让客户端把已回滚的号段投入使用，下一次AllocateId重复发放同一号段。
		// 对齐GCM-raft的proc.autoResponse（响应由_final_commit_在appendLog之后发出）；
		// result已填的startId/count在回滚路径随错误码一起发送，客户端按resultCode!=0丢弃。
		// 非事务上下文（不应发生）保持立即应答。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	// 只写session上一个int（对齐非raft版：无状态簿记、无取消语义）。虽然走raft请求通道，
	// 但不写rocks表，不产生共识复制；换leader后agent重新Login时会重发Identify。
	@Override
	protected long ProcessIdentifyRequest(Identify r) {
		var netSession = (Session)r.getSender().getUserState();
		if (netSession != null) {
			netSession.identifyServerId = r.Argument.serverId;
			logger.info("{}: Identify serverId={}", r.getSender(), r.Argument.serverId);
		}
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
		var loadObservers = tableLoadObservers.getOrAdd(r.Argument.ip + "_" + r.Argument.port);
		var observers = loadObservers.getObservers();

		var set = new SetServerLoad();
		set.Argument = r.Argument;

		ArrayList<String> removed = null;
		for (var observer : observers) {
			try {
				var session = tableSession.get(observer);
				if (null != session && set.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId())))
					continue;
			} catch (Throwable ignored) { // ignored
			}
			if (removed == null)
				removed = new ArrayList<>();
			removed.add(observer);
		}
		if (removed != null) {
			for (var remove : removed)
				observers.remove(remove);
		}
		// 应答必须raft提交成功后发出（对齐ProcessAllocateIdRequest的修复2eee0da1d）：
		// observers的死条目清理依赖raft提交，提交前应答在复制失败回滚后客户端已拿到成功码。
		// set.Send的负载转发是对观察者的数据推送（无状态语义），保持在handler内立即发送。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
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

	// 对齐非raft版ServiceManagerServer.isLegalServiceIdentity（FND-S2-6）：
	// 非'@'/'#'前缀必须是可Long.parseLong的数字，否则订阅者侧BServiceInfos.comparer
	// 在排序上抛NumberFormatException，打断同批全部合法变更的处理。
	private static boolean isLegalServiceIdentity(@NotNull String identity) {
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

	// 未Login会话统一拒绝码（FND4-60）：Procedure保留码用到-17，本模块局部取-18。
	public static final long ErrorNotLogin = -18;

	/**
	 * 会话前置条件单点强制（FND4-60）：未Login的连接（userState非Session，或Login事务刚被
	 * 回滚/清理的窗口——见FND4-57）发Edit/Subscribe/UnSubscribe时tableSession.get(name)
	 * 为null，原实现三处直接解引用NPE（错误码不明确、日志噪声、恶意可稳定触发服务端NPE
	 * 路径）。统一应答ErrorNotLogin并返回null，调用方立即返回。
	 */
	private @Nullable Session requireSession(@NotNull Zeze.Net.Rpc<?, ?> r) {
		if (!(r.getSender().getUserState() instanceof Session netSession))
			return notLogin(r);
		return tableSession.get(netSession.name) != null ? netSession : notLogin(r);
	}

	private @Nullable Session notLogin(@NotNull Zeze.Net.Rpc<?, ?> r) {
		r.SendResultCode(ErrorNotLogin);
		return null;
	}

	@Override
	protected long ProcessEditRequest(Edit r) {
		// 服务端注册入口校验identity（FND-S2-6，对齐非raft版isLegalServiceIdentity）：
		// 非法identity会令订阅者侧BServiceInfos.comparer的Long.parseLong抛NumberFormatException，
		// 打断同批全部合法变更的处理。畸形请求整批拒绝（错误码经派发层onError应答）。
		for (var info : r.Argument.getAdd())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Zeze.Transaction.Procedure.ErrorRequestId;
		for (var info : r.Argument.getRemove())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Zeze.Transaction.Procedure.ErrorRequestId;
		var netSession = requireSession(r); // FND4-60
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var notifies = new HashMap<AsyncSocket, Edit>();

		// step 1: remove
		for (var unReg : r.Argument.getRemove()) {
			var state = tableServerState.get(unReg.getServiceName());
			if (state != null)
				removeAndCollectNotifyAllVersions(state, unReg.getServiceIdentity(), netSession.name, notifies);
			var session = tableSession.get(netSession.name);
			session.getRegisters().remove(toRocksKey(unReg)); // ignore remove failed
		}

		// step 2: add
		for (var reg : r.Argument.getAdd()) {
			var session = tableSession.get(netSession.name);
			// 允许重复登录，断线重连Agent不好原子实现重发。
			session.getRegisters().put(toRocksKey(reg), toRocks(reg, netSession.name));
			var state = tableServerState.getOrAdd(reg.getServiceName());
			if (!state.getServiceName().equals(reg.getServiceName()))
				state.setServiceName(reg.getServiceName());
			addAndCollectNotify(state, reg, netSession.name, notifies);
		}

		// 应答与订阅者通知必须raft提交成功后发出（对齐ProcessAllocateIdRequest的修复2eee0da1d）：
		// appendLog之前发送，raft复制失败回滚时客户端已拿到成功码、订阅者已收到幽灵
		// add/remove推送，而服务端状态回滚（提交前应答，状态不一致）。非事务上下文（不应
		// 发生）保持立即应答。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
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
		// BEditService.add声明AddOrUpdate以name+id为key：同identity重注册到新版本时，
		// 先从其他版本桶移除旧记录并通知其版本订阅者remove（对齐非raft版ServiceManagerServer），
		// 否则实例下线后旧版本桶残留幽灵地址（会话registers以name+id为key只保留最后一次注册）。
		for (var e : state.getServiceInfosVersion().entrySet()) {
			if (e.getKey() == info.getVersion())
				continue;
			// CollMap2.remove 返回 void：先取旧值再移除，有旧值才通知其版本订阅者。
			var old = e.getValue().getServiceInfos().get(info.getServiceIdentity());
			if (old != null) {
				e.getValue().getServiceInfos().remove(info.getServiceIdentity());
				collectNotify(state, fromRocks(old), false, notifies);
			}
		}
		var versions = state.getServiceInfosVersion().get(info.getVersion());
		if (null == versions)
			state.getServiceInfosVersion().put(info.getVersion(), versions = new BServiceInfosVersionRocks());
		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		versions.getServiceInfos().put(info.getServiceIdentity(), toRocks(info, sessionName));
		collectNotify(state, info, true, notifies);
		// 新注册实例同样要为现有订阅者登记负载观察者（FND-S1-8）：addLoadObserver此前只在
		// 订阅时登记，观察者集合是订阅时刻的快照——订阅之后注册的实例，其负载上报永不转发
		// 给订阅者（权重缺失直到重连重订阅）。getSimple的key即订阅会话名；本方法在
		// ProcessEditRequest事务内，与订阅/退订的simple修改经raft单写者串行，迭代安全。
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
		var netSession = requireSession(r); // FND4-60
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var session = tableSession.get(netSession.name);
		for (var info : r.Argument.subs) {
			session.getSubscribes().put(info.getServiceName(), toRocks(info));
			var state = tableServerState.getOrAdd(info.getServiceName());
			if (!state.getServiceName().equals(info.getServiceName()))
				state.setServiceName(info.getServiceName());
			subscribeAndCollect(state, r, info, netSession.name);
		}
		// 应答必须raft提交成功后发出（对齐ProcessAllocateIdRequest的修复2eee0da1d）：
		// 会话subscribes与state.simple的写入、Result.map的快照依赖raft提交，提交前应答在
		// 复制失败回滚后客户端已拿到成功码与快照（假成功），订阅未生效将收不到增量推送。
		// 回滚路径Result已填的map随错误码一起发送，客户端按resultCode!=0丢弃。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
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

	// 注销以name+id为key跨全部版本桶收敛（与addAndCollectNotify、非raft版onClose一致）：
	// 仅移除属于本会话的记录；归属不符时不删不通知（新会话的AddOrUpdate注册不被静默删除）。
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
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var netSession = requireSession(r); // FND4-60
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var session = tableSession.get(netSession.name);
		for (var serviceName : r.Argument.serviceNames) {
			var sub = session.getSubscribes().get(serviceName);
			session.getSubscribes().remove(serviceName);
			if (sub != null) {
				unSubscribeNow(netSession.name, serviceName);
			}
		}
		// 应答必须raft提交成功后发出（对齐ProcessAllocateIdRequest的修复2eee0da1d）：
		// 会话subscribes与state.simple的移除依赖raft提交，提交前应答在复制失败回滚后
		// 客户端已拿到成功码（假成功），实际订阅仍生效。
		// RocksRaft版事务（FND2-S1-1）：本派发链（dispatchRaftRequest→Procedure.call）只创建
		// RocksRaft事务，Zeze.Transaction.Transaction.getCurrent()在此恒为null——那会令
		// runWhileCommit永不注册、应答退化为handler内立即发送（提交前应答=假成功）。
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	public BServerState unSubscribeNow(String sessionName, String serviceName) {
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

		var netSession = (Session)r.getSender().getUserState();
		for (var versions : state.getServiceInfosVersion().values())
			for (var info : versions.getServiceInfos().values())
				addLoadObserver(info.getPassiveIp(), info.getPassivePort(), netSession.name);
	}
}
