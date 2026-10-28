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
	// 会话清理对账周期守护：body进worker池不占调度线程，close时有界等待在飞一轮。
	private final DaemonTimer reconcileDaemon = new DaemonTimer("ServiceManagerWithRaft.reconcileSessions", 60_000, this::reconcileSessions);
	// close标记：置位后，SM锁串行的raft侧任务体（dispatchRaftRequest/RpcResponse、closeSession、
	// reconcileSessions）在锁内首查即退出，不再触碰rocks。与close()的锁屏障配合，见close()。
	private volatile boolean closed;
	private final Table<String, BAutoKey> tableAutoKey;
	private final Table<String, BSession> tableSession;
	private final Table<String, BLoadObservers> tableLoadObservers;
	private final Table<String, BServerState> tableServerState;

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf conf = new ServiceManagerServer.Conf();

	public ServiceManagerWithRaft(String raftName, @Nullable RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, Config.load(), false);
	}

	public ServiceManagerWithRaft(String raftName, @Nullable RaftConfig raftConf, @Nullable Config config,
								  boolean RocksDbWriteOptionSync) throws Exception {
		ZezeCounter.tryInit();
		ServiceManagerServer.applyLogLevelProperty(); // 显式启动动作（仅显式指定logLevel属性才动配置）

		if (config == null)
			config = Config.load();
		config.parseCustomize(conf);

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync,
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
		// 关停清理：取消全部Takeover梯队升档定时器（900s级长定时器挂着会拖死JVM收尾）。
		for (var tier : suspectTiers.values())
			if (tier.timer != null)
				//noinspection DataFlowIssue
				tier.timer.cancel(false);
		suspectTiers.clear();
		reconcileDaemon.stop(); // 有界等待在飞一轮（预算=timeoutMs+5s）；超预算逃逸轮由closed+锁屏障挡
		closed = true;
		// SM锁是raft侧所有存储过程体的串行点且全程持有：先置closed再过一次锁屏障——拿到锁时
		// 已无过程体在执行，此后排定的任务体在锁内见closed直接退出。否则rocks.close()释放的
		// 原生句柄可与在途的walk/iterator竞争，librocksdbjni段错误直接杀死整个JVM（fast套件
		// 偶发崩溃的根因：closeSession退避重试排定在全局critical池上，最多12.8s后才跑）。
		// 屏障后不持锁调rocks.close()：close路径内raft.shutdown要停网络，若有IO线程正阻塞在
		// SM锁上（响应派发），持锁等待可能与停机互等。
		lock();
		unlock();
		rocks.close();
	}

	// leader周期对账：清理"连接已死但清理事务未落地"的残留会话行。
	// closeSession的退避重试只覆盖"断连节点恢复多数派/重新当选"的场景；leader切换后，
	// 断连风暴的清理在旧leader上永远RaftRetry，进程崩溃则内存待办全丢——对账在现任
	// leader上周期收敛这两类残余（死agent的注册/订阅行、订阅方的幽灵地址）。
	// SM锁内执行，与Login/OnSocketClose串行；walk快照与清理事务之间行被新连接接管时，
	// cleanupSessionRow的归属校验兜底，不会误清。对账事务失败仅记error，下轮周期自带重试。
	private void reconcileSessions() throws Exception {
		var raft = rocks.getRaft();
		lock();
		try {
			// closed与isWorkingLeader同在锁内：closed先于close()锁屏障置位，屏障后获取锁的
			// 轮次见closed即退出——isWorkingLeader在锁外先摸rocks会撞"过墓碑后关库"的竞态窗口。
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
			// remove通知必须raft提交成功后发出：appendLog之前发送时，closeSession/reconcileSessions的
			// RaftRetry重跑每试一次就重发一批Edit(remove)（订阅者端过滤为无害但成噪声），
			// 8连败终败则订阅者已删而tServerState残留（新旧订阅者视图分叉窗口）。提交后发送
			// 维持"订阅者视图⊆已提交的服务端状态"。非事务上下文（不应发生）保持立即发送。
			var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
			if (t != null)
				t.runWhileCommit(() -> sendNotifies(notifies));
			else
				sendNotifies(notifies);
		}
		// 会话关闭联动清理该会话登记的负载观察者：仅靠setLoad转发失败时的惰性剔除，
		// 停止上报的地址行（raft持久表）与死观察者会永久残留（无界增长）。observers清空即删地址行。
		// walk返回detached解码拷贝（RocksRaft.Table.walk不附着事务），对拷贝的remove不落库：
		// 只收集命中key，修改必须经getOrAdd拿事务附着bean再remove。
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
						if (closed)
							return Zeze.Transaction.Procedure.RaftRetry;
						if (logger.isDebugEnabled()) {
							//noinspection DataFlowIssue
							var netSession = (Session)p.getSender().getUserState();
							var ssName = null != netSession ? netSession.name : "";
							logger.debug("dispatchRaftRequest: {}@{}{}", p.getClass().getName(), ssName, p);
						}
						var procedure = new Procedure(rocks, func);
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

		// 会话清理的raft提交结果必须闭环：RaftRetry是返回码不是异常
		// （RocksRaft Transaction.perform捕获RaftRetryException后返回Procedure.RaftRetry），
		// 曾经的procedure.call()返回码被忽略——leader切换既造成agent断连风暴（旧leader上
		// OnSocketClose批量触发）又恰使旧leader的appendLog失败，回滚后的清理
		// （注销/退订/删行/发remove通知）不重试不告警，死agent的注册与订阅在raft表中
		// 永久残留，订阅方持有幽灵地址持续分发。onClose幂等（归属校验后清理、
		// 不存在即no-op、Suspect为提示性重发），失败退避重试，上限后fatal留观测。
		// 残余缺口=进程崩溃窗口内的清理丢失（无持久化待办），周期对账兜底另立项。
		private void closeSession(Session netSession, int retry) {
			// 取消KeepAlive定时器先于closed门禁——close()置closed后本方法
			// 的清理事务不再落地（重启由对账收敛），但定时器若不取消，rocks.close()的
			// setRaft(null)后任务体每tick对其裸解引用NPE（周期任务显式吞异常永续，对齐
			// 非raft版OnSocketClose同步取消的语义）。cancelKeepAlive幂等，重复调用无害。
			netSession.cancelKeepAlive();
			// 同 dispatchRaftRequest：清理的raft提交不能在 IO 线程上等待。
			Raft.executeImportantTask(() -> {
				lock();
				try {
					if (closed)
						return; // 服务已关闭：清理不再落地（重启后由对账收敛），也不再重试
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
					// socket已不存在（会话断开，GetSocket返回null）：正常断开态而非错误，
					// 无从发送也无需关闭，会话清理由OnSocketClose负责；null穿透到Send失败
					// 分支的sock.close直接NPE，被外层catch吞成周期性error日志。
					if (s == null)
						return;
					try {
						var r = new KeepAlive();
						// 异步等待应答（对齐非raft版）：SendAndWaitCheckResultCode在
						// 调度池线程上同步阻塞，半开连接堆积时可耗尽调度池拖停全部周期任务。
						// 回调判活：超时/失败码在回调中关闭连接触发重连。
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

		// 取消keepAlive周期任务。除onClose外，重复Login覆盖socket的userState前也必须调用：
		// 被覆盖的旧Session的onClose永不执行，其定时器永不取消。
		public void cancelKeepAlive() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);
		}

		public void onClose() {
			cancelKeepAlive();

			// Suspect梯队广播（首档==死者version；档间超时升序逐档；低于死者version的永不通知；
			// TakeoverDone回执即停）。仅是提示（hint），接收方转化为takeover.tryTransfer，由租约表
			// 裁决；未过期租约会被安排到过期时刻精确重试。短暂掉线误判不存在：接管前租约必须
			// 过期，死者重启会claim新epoch。
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
		// sessionName是tSession主键兼会话归属凭证，空白名（<ServiceManagerConf>
		// 漏配sessionName属性时Config解析为空串）会使多个server共享同一会话行互相接管——
		// 推送错乱、断连连带注销对方注册订阅，表现为服务闪断。非空为部署硬约束（唯一性
		// 仍为部署契约，见Config.ServiceManagerConf），拒绝时不得产生会话行/userState副作用。
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
		// 入口校验（同非raft版ServiceManagerServer与UDP面Id128UdpServer口径）：
		// 该端口无认证（raft对等端口同时服务应用协议，四种EncryptType均密钥协商），name/count
		// 均对端可控，无界唯一name直接成为tAutoKey持久键并经共识复制放大到全节点。
		//noinspection DataFlowIssue
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX
			|| name.getBytes(StandardCharsets.UTF_8).length > 128) {
			//noinspection DataFlowIssue
			warnAllocateIdRejected("invalid count=" + count + " name.chars=" + name.length());
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		// 唯一名上限。raft表逐出不可移植：BAutoKey.Current是已交付位置（无独立
		// 高水位），删行重建会从默认值重发已交付号段——超限只能拒绝。运维出口：确认攻击后
		// 清理tAutoKey表攻击行。满员会暂时锁死新合法name（仓内合法基数个位到两位数，
		// 1024上限余量充分）。walk计数O(表行数)，表行数被上限封顶，可接受。
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

		// 号段必须raft提交成功后再应答：appendLog失败（失主RaftRetry/复制异常）会回滚current，
		// 提交前应答会让客户端把已回滚的号段投入使用，下一次AllocateId重复发放同一号段。
		// 对齐GCM-raft的proc.autoResponse（响应由_final_commit_在appendLog之后发出）；
		// result已填的startId/count在回滚路径随错误码一起发送，客户端按resultCode!=0丢弃。
		// 非事务上下文（不应发生）保持立即应答。
		var t = Zeze.Raft.RocksRaft.Transaction.getCurrent();
		if (t != null)
			t.runWhileCommit(r::SendResult);
		else
			r.SendResult();
		return 0;
	}

	// 拒绝告警限频（同Id128UdpServer.warnRejected）：无认证端口上高频非法
	// 请求按包记日志可耗尽日志盘/CPU（日志刷屏DoS），60秒一条。
	private volatile long lastAllocateIdRejectLogMs;

	private void warnAllocateIdRejected(String reason) {
		var now = System.currentTimeMillis();
		var last = lastAllocateIdRejectLogMs;
		if (now - last < 60_000)
			return; // 限频窗口内静默拒绝（race下至多多记几条）
		lastAllocateIdRejectLogMs = now;
		logger.error("AllocateId(raft) rejected (possible attack or misbehaving client): {}", reason);
	}

	// Edit/Subscribe/SetServerLoad入口上限常量与helper，
	// 阈值与非raft版一致（见ServiceManagerServer同名常量注释）。
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
			return; // 限频窗口内静默拒绝（race下至多多记几条）
		lastSvcRejectLogMs = now;
		logger.error("SM(raft) edit/subscribe rejected (possible attack or misbehaving client): {}", reason);
	}

	/**
	 * 全局唯一serviceName上限（事务内调用，raft单写者串行）。行不存在且计数达
	 * 上限时先扫除空壳行（无注册无订阅——内容可由客户端重发恢复，非发号器状态删行
	 * 安全，区别于tAutoKey的只拒不删）腾位；无空壳可扫才拒绝。walk返回detached解码
	 * 拷贝（RocksRaft.Table.walk不附着事务），只收集key、修改经remove落库——
	 * 同cleanupSessionRow的负载观察者清理。
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

	// 只写session上一个int（对齐非raft版：无状态簿记、无取消语义）。虽然走raft请求通道，
	// 但不写rocks表，不产生共识复制；换leader后agent重新Login时会重发Identify。
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

	// ===== Takeover version 梯队通知（语义与实现要点对齐非raft版 ServiceManagerServer） =====
	// ①首档==死者version；②档间超时无TakeoverDone回执升到更高存活version的最小值；③低于
	// 死者version的永不通知；④回执即停。失败界定=超时（重复Suspect由已接管者幂等空转消化）；
	// 内存态：leader切换/进程重启即失，由GS侧Takeover.scan周期兜底。
	// 档间超时(毫秒)：默认900s=1.5×GS的TakeoverTtl默认值(600s)，GS的Ttl改小时应同比调整。
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
		var suspect = new Suspect();
		suspect.Argument.serverId = deadServerId;
		suspect.Argument.version = deadVersion;
		try {
			//noinspection DataFlowIssue
			rocks.getRaft().getServer().foreach(so -> {
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

	/** onClose入口：广播首档（==死者version）并安排升档链。 */
	private void startSuspectTier(int deadServerId, long deadSessionId, long deadVersion) {
		var tier = new SuspectTier(deadVersion, deadVersion);
		var old = suspectTiers.put(deadServerId, tier);
		if (old != null && old.timer != null)
			//noinspection DataFlowIssue
			old.timer.cancel(false); // 死者反复闪断：新周期覆盖旧升档链
		broadcastSuspectTier(deadServerId, deadSessionId, deadVersion, deadVersion);
		scheduleNextTier(deadServerId, deadSessionId, tier);
	}

	/** 档间超时：无回执升到"更高存活version的最小值"；没有更高档=放弃（scan兜底）。 */
	private void scheduleNextTier(int deadServerId, long deadSessionId, SuspectTier tier) {
		tier.timer = TaskSpec.ofAction(() -> {
			var cur = suspectTiers.get(deadServerId);
			if (cur != tier)
				return; // 已被回执移除/被新周期覆盖
			var next = new long[]{Long.MAX_VALUE};
			final var deadSessionIdRef = deadSessionId;
			try {
				//noinspection DataFlowIssue
				rocks.getRaft().getServer().foreach(so -> {
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
			if (next[0] == Long.MAX_VALUE) {
				suspectTiers.remove(deadServerId, cur); // 没有更高档：放弃升档
				logger.info("Suspect tier exhausted for serverId={} lastTier={}", deadServerId, cur.tierVersion);
				return;
			}
			cur.tierVersion = next[0];
			broadcastSuspectTier(deadServerId, deadSessionIdRef, cur.deadVersion, next[0]);
			scheduleNextTier(deadServerId, deadSessionIdRef, cur);
		}).scheduleNow(takeoverNotifyStepMs);
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
		// SetServerLoad需Login门禁与ip长度校验：未认证可达即可用任意ip+port建
		// tLoadObservers持久行，空observers行永不命中cleanupSessionRow的Contains清理。
		// get化——无观察者行则忽略本次上报（零状态变更，行创建权收归Login后的
		// addLoadObserver闭环），不产生新空行。
		if (isOverUtf8Bytes(r.Argument.ip, SVC_IP_MAX_BYTES)) {
			warnSvcRejected("setLoad ip over size");
			return Zeze.Transaction.Procedure.ErrorRequestId;
		}
		var netSession = requireSession(r);
		if (netSession == null)
			return 0; // 未Login已应答ErrorNotLogin
		var loadObservers = tableLoadObservers.get(r.Argument.ip + "_" + r.Argument.port);
		if (loadObservers == null)
			return 0; // 无观察者：忽略上报，不建行
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
		// 应答必须raft提交成功后发出：observers的死条目清理依赖raft提交，提交前应答在复制失败回滚后客户端已拿到成功码。
		// set.Send的负载转发是对观察者的数据推送（无状态语义），保持在handler内立即发送。
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

	// 对齐非raft版ServiceManagerServer.isLegalServiceIdentity：
	// 非'@'/'#'前缀必须是可Long.parseLong的数字，否则订阅者侧BServiceInfos.comparer
	// 在排序上抛NumberFormatException，打断同批全部合法变更的处理。
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
		// 服务端注册入口校验identity（对齐非raft版isLegalServiceIdentity）：
		// 非法identity会令订阅者侧BServiceInfos.comparer的Long.parseLong抛NumberFormatException，
		// 打断同批全部合法变更的处理。畸形请求整批拒绝（错误码经派发层onError应答）。
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

		// 字段长度/批/每会话/全局唯一名上限（raft侧经共识复制放大到
		// 全节点+WAL，tServerState行壳跨重启永不清理）。阈值与非raft版一致；raft行
		// 虽是持久表但内容（注册/订阅）可由客户端重发恢复——满员时事务内扫除空壳行
		// 腾位（非发号器状态，删行安全，区别于tAutoKey的只拒不删），无空壳才拒绝。
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
		var sessionRow = tableSession.get(netSession.name);
		var finalRegisters = new HashSet<>(sessionRow.getRegisters().keys());
		var removeKeys = new HashSet<BServiceInfoKeyRocks>();
		for (var info : r.Argument.getRemove()) {
			var key = toRocksKey(info);
			finalRegisters.remove(key);
			removeKeys.add(key);
		}
		var requestedNames = new HashSet<String>();
		for (var info : r.Argument.getAdd()) {
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
		for (var reg : r.Argument.getAdd()) {
			var session = tableSession.get(netSession.name);
			// 允许重复登录，断线重连Agent不好原子实现重发。
			//noinspection DataFlowIssue
			session.getRegisters().put(toRocksKey(reg), toRocks(reg, netSession.name));
			var state = tableServerState.getOrAdd(reg.getServiceName());
			if (!state.getServiceName().equals(reg.getServiceName()))
				state.setServiceName(reg.getServiceName());
			addAndCollectNotify(state, reg, netSession.name, notifies);
		}

		// 应答与订阅者通知必须raft提交成功后发出：appendLog之前发送，raft复制失败回滚时
		// 客户端已拿到成功码、订阅者已收到幽灵add/remove推送，而服务端状态回滚（提交前
		// 应答，状态不一致）。非事务上下文（不应发生）保持立即应答。
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
		pruneEmptyVersions(state);
		var versions = state.getServiceInfosVersion().get(info.getVersion());
		if (null == versions)
			state.getServiceInfosVersion().put(info.getVersion(), versions = new BServiceInfosVersionRocks());
		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		versions.getServiceInfos().put(info.getServiceIdentity(), toRocks(info, sessionName));
		collectNotify(state, info, true, notifies);
		// 新注册实例同样要为现有订阅者登记负载观察者：addLoadObserver若只在订阅时登记，
		// 观察者集合是订阅时刻的快照——订阅之后注册的实例，其负载上报永不转发
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
		// 应答必须raft提交成功后发出：会话subscribes与state.simple的写入、Result.map的快照依赖raft提交，
		// 提交前应答在复制失败回滚后客户端已拿到成功码与快照（假成功），订阅未生效将收不到增量推送。
		// 回滚路径Result已填的map随错误码一起发送，客户端按resultCode!=0丢弃。
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
		// 应答必须raft提交成功后发出：会话subscribes与state.simple的移除依赖raft提交，提交前应答在
		// 复制失败回滚后客户端已拿到成功码（假成功），实际订阅仍生效。
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
