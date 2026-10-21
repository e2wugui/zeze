package Zeze.Services;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
import Zeze.Builtin.ServiceManagerWithRaft.Identify;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Suspect;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Raft.Agent;
import Zeze.Raft.RaftConfig;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.DispatchModeAnnotation;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Raft版服务管理器客户端Agent：经raft连接ServiceManagerWithRaft，注册/订阅断线重连后
 * 自动重放，不支持Id128 UDP发号（History组合构造即fail-fast）。
 */
public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	private static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Agent raftClient;
	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	// 断线（换leader）重连后重放用：服务端行按name持久化，但flap时行可能已被onClose删除，
	// 重放注册/订阅才能恢复服务端状态（对齐非raft版Agent.onConnected）。
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	@Override
	public Threading getThreading() {
		throw new UnsupportedOperationException();
	}

	public ServiceManagerAgentWithRaft(Config config) throws Exception {
		// raft版不支持Id128 UDP发号（tid128UdpClient不初始化），而开启History的事务在
		// finalCommit（getUsableTid128CacheFuture）/Tid128Cache.next两个入口都依赖它：
		// 该组合下写事务全量NPE失败、热事务finalCommit失败直接halt(543543)。
		// 不支持的组合在构造时明确报错（fail-fast），而非运行期以NPE/halt形态失败。
		if (config.isHistory())
			throw new IllegalStateException("ServiceManager=raft does not support Id128 allocate: " +
					"History('" + config.getHistory() + "') requires it. " +
					"Use a non-raft ServiceManager or disable History.");
		// 空白sessionName（漏配sessionName属性时解析为空串）会与其它同样漏配的
		// server共享会话行互相接管（服务闪断，见ServiceManagerWithRaft.ProcessLoginRequest）。
		// 经Application+ServiceManager=raft启动已默认为projectName#serverId；直接构造要求显式
		// 配置非空名字——启动即失败优于连上后被服务端拒绝或与同名者互踩。
		var smConf = config.getServiceManagerConf();
		if (smConf == null || smConf.getSessionName().isBlank())
			throw new IllegalStateException("ServiceManagerConf.sessionName must not be blank " +
					"(session name must be unique per server). " +
					"Configure <ServiceManagerConf sessionName=\"...\"/> or set it via setSessionName().");
		super.config = config;

		var raftConf = RaftConfig.load(config.getServiceManagerConf().getRaftXml());
		raftClient = new Agent("servicemanager.raft", raftConf, config);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		raftClient.dispatchProtocolToInternalThreadPool = true;
		RegisterProtocols(Objects.requireNonNull(raftClient.getClient()));
		// 不初始化tid128UdpClient（raft版不支持Id128 UDP发号）：不支持组合已在构造fail-fast拦截。
	}

	private void raftOnSetLeader(Agent agent) {
		// 直接使用自身持有的config。raftClient以Config构造（无Application），getClient().getZeze()为null，
		// 经zeze round-trip取配置会在null检查处直接return，Login永远不发送。
		var future = startNewLogin();
		var login = new Login();
		//noinspection DataFlowIssue
		login.Argument.setSessionName(config.getServiceManagerConf().getSessionName());

		agent.send(login, p -> {
			var rpc = (Login)p;
			if (rpc.isTimeout())
				raftOnSetLeader(agent);
			else if (rpc.getResultCode() != 0) {
				logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
			} else {
				future.setResult(true);
				// 异步重放，不在rpc回调线程里阻塞等待重放的响应。
				TaskSpec.ofAction(replay::run).name("ServiceManager.AgentWithRaft.OnLoginSuccess").run();
			}
			return 0;
		});
	}

	/**
	 * 每次登录成功及重试时上报 Identify、重放全部注册；注册成功后由 ServiceReplay 恢复订阅。
	 * 服务端幂等（AddOrUpdate、允许重复注册），重复重放无害。
	 */
	private void replayRegisters() {
		// 先上报Identify：SM据此把serverId记在会话上，断线时广播Suspect（对齐非raft版onConnected）。
		// 重发=恢复提示资格（与正确性无关，正确性由Takeover租约裁决）。
		try {
			var identify = new Identify();
			//noinspection DataFlowIssue
			identify.Argument.serverId = config.getServerId();
			identify.Argument.version = config.getAppMainVersion();
			raftClient.send(identify, __ -> 0L);
		} catch (Throwable ex) {
			logger.error("OnLoginSuccess.Identify", ex);
		}

		synchronized (editServiceLock) {
			var edit = new BEditService();
			edit.getAdd().addAll(registers.values());
			if (!edit.getAdd().isEmpty())
				editService(edit);
		}
	}

	private final ServiceReplay replay = new ServiceReplay(this::replayRegisters,
			"SM.AgentWithRaft.loginReplayRetry", Level.ERROR);

	/// /////////////////////////////////////////////////////////////////////
	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) {
		if (onKeepAlive != null)
			Objects.requireNonNull(Task.getCriticalThreadPool()).execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	// Suspect仅是提示：转化为onSuspect回调（应用接takeover.tryTransfer），
	// 租约未过期时tryTransfer内部安排到过期时刻精确重试，不会误接管。
	@Override
	protected long ProcessSuspectRequest(Suspect r) {
		notifySuspect(r.Argument.serverId, r.Argument.version);
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	public void sendTakeoverDone(int deadServerId) {
		try {
			var done = new Zeze.Builtin.ServiceManagerWithRaft.TakeoverDone();
			done.Argument.deadServerId = deadServerId;
			//noinspection DataFlowIssue
			done.Argument.takerServerId = config.getServerId();
			done.Argument.takerVersion = config.getAppMainVersion();
			raftClient.send(done, __ -> 0L);
		} catch (Throwable ex) {
			logger.debug("sendTakeoverDone", ex);
		}
	}

	// Direct：Edit推送与Subscribe应答（dispatchRpcResponse内联）同在IO线程按TCP接收序串行应用，
	// 对齐非raft版Agent的Direct注册。Edit增量无序号，乱序应用会令订阅状态与注册表永久分叉
	// ；本handler体内全为非阻塞操作（CHM、SubscribeState锁、SendResult异步发、
	// triggerOnChanged投oneByOne池），内联执行不阻塞IO线程。
	@Override
	@DispatchModeAnnotation(mode = DispatchMode.Direct)
	protected long ProcessEditRequest(Edit r) {
		applyServiceEdit(r.Argument);

		r.SendResult();
		try {
			triggerOnChanged(r.Argument);
		} catch (Throwable e) {
			logger.error("ProcessEditRequest: triggerOnChanged exception:", e);
		}
		return Procedure.Success;
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) {
		loads.put(r.Argument.getName(), r.Argument);
		if (onSetServerLoad != null) {
			Objects.requireNonNull(Task.getCriticalThreadPool()).execute(() -> {
				try {
					var onSetLoad = onSetServerLoad;
					if (onSetLoad != null) {
						onSetLoad.run(r.Argument);
					}
				} catch (Throwable e) {
					logger.error("", e);
				}
			});
		}
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected boolean allocateAsync(String globalName, int allocCount,
									ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
		if (allocCount < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(globalName);
		r.Argument.setCount(allocCount);
		raftClient.send(r, (p) -> {
			try {
				return callback.handle(r);
			} catch (Exception ex) {
				throw Task.forceThrow(ex);
			}
		});
		return true;
	}

	@Override
	protected void allocate(AutoKey autoKey, int pool) {
		if (pool < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(pool);
		raftClient.sendForWait(r).await();
		// 真错误码时Result携带的号段来自服务端回滚路径（提交前数据，服务端按错误码丢弃语义发送），
		// 不可投入使用，抛错使AutoKey.next()的重试循环以异常退出而非无限重试；RaftApplied例外，
		// 其Result是重发命中去重时服务端附带的原始已分配号段，直接投入使用。
		checkResultCode(r);
		setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	/**
	 * 对齐非raft版SendAndWaitCheckResultCode契约：sendForWait的future只在超时/停止时异常完成，
	 * 服务端错误码应答正常完成，必须显式检查；失败即抛错，防止editService/unSubscribeService
	 * 假成功（本地状态与服务端分叉）及allocate静默失败触发AutoKey.next()无限循环。
	 * RaftApplied放行为成功：应答丢失/超时后sendForWait重发，原请求已apply时服务端以
	 * RaftApplied附原始结果应答（Raft.Server.processRequest去重命中），与Dbh2Agent等
	 * raft调用方"rc==0||rc==RaftApplied"的成功判定一致；allocate据此拿回原号段。
	 */
	private static void checkResultCode(Rpc<?, ?> r) {
		var rc = r.getResultCode();
		if (rc != 0 && rc != Procedure.RaftApplied)
			throw new IllegalStateException("Rpc Invalid ResultCode=" + rc + " " + r);
	}

	private void waitLoginReady() {
		//noinspection DataFlowIssue
		var deadline = System.currentTimeMillis() + super.config.getServiceManagerConf().getLoginTimeout();
		for (; ; ) {
			var volatileTmp = loginFuture;
			// await超时或被取消都返回false，此时不能再调get()：未完成的future上get()会无限期park，
			// 下面的deadline检查将不可达；被取消则重读最新loginFuture继续等。
			if (volatileTmp.isDone() || volatileTmp.await(Math.max(1, deadline - System.currentTimeMillis()))) {
				try {
					if (Boolean.TRUE.equals(volatileTmp.get())) // 到这里future已完成，get()不会park。
						return;
				} catch (Throwable ignored) {
					// 等待期间raftOnSetLeader执行startNewLogin，cancel旧future并替换；
					// 被替换不是失败，重读最新future继续等。
				}
			}
			if (System.currentTimeMillis() >= deadline)
				throw new IllegalStateException("login timeout.");
		}
	}

	private TaskCompletionSource<Boolean> startNewLogin() {
		lock();
		try {
			loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
			return loginFuture = new TaskCompletionSource<>();
		} finally {
			unlock();
		}
	}

	// editService专用串行锁（对齐非raft版ServiceManager.Agent.editServiceLock）：
	// 把"变更本地registers-发送-等待应答-提交/回滚"全过程串行化，且replayRegisters的重放
	// 快照在同一锁内取——快照因此只能看到已确认的完整状态，不会捕获"本地已变更、远端未
	// 确认随后被回滚"的中间态（local-first窗口下重放投递成功+原edit失败回滚
	// =僵尸注册复现）。串行化同时消除同key并发edit的交错残留（AbstractAgent类注释豁免项）。
	// 不复用__thisLock：后者被startNewLogin使用，复用会把换leader登录与edit互相阻塞；
	// 本锁内等待loginFuture/RPC应答但不获取__thisLock，全局无反序路径。
	private final Object editServiceLock = new Object();

	@Override
	public void editService(BEditService arg) {
		synchronized (editServiceLock) {
			for (var info : arg.getAdd())
				verify(info.getServiceIdentity());
			// 先更新本地记录再发送远程请求（重连重放的数据来源）。失败时回滚本次真实变更
			// ：重放只有add语义（replayRegisters全量addAll，无remove），remove失败的
			// 条目若不回滚——本地已删、服务端永续残留，连接存活期间无人再发注销，僵尸注册
			// 持续分发流量。只记录本次真实变更（新增/覆盖旧值/真实删除）；重放路径的幂等put
			// 键已存在（prev==reg），不属于变更，回滚不得清空重放源。
			var added = new ArrayList<BServiceInfo>();
			var replaced = new java.util.ArrayList<Zeze.Util.KVNotNull<BServiceInfo, BServiceInfo>>();
			var removed = new java.util.ArrayList<Zeze.Util.KVNotNull<BServiceInfo, BServiceInfo>>();
			for (var unReg : arg.getRemove()) {
				var old = registers.remove(unReg);
				if (old != null)
					removed.add(Zeze.Util.KVNotNull.create(unReg, old));
			}
			for (var reg : arg.getAdd()) {
				var prev = registers.put(reg, reg);
				if (prev == null)
					added.add(reg);
				else if (prev != reg)
					replaced.add(Zeze.Util.KVNotNull.create(reg, prev)); // 同key新版本对象，回滚需还原旧值
			}
			try {
				waitLoginReady();

				var edit = new Edit(arg);
				raftClient.sendForWait(edit).await();
				checkResultCode(edit); // 失败即抛错并回滚本地：调用方知情后重试整个edit，
				// 与非raft版"成功后才更新本地"语义对齐（Agent.java）。
				logger.debug("EditService {}", arg);
			} catch (Throwable ex) {
				// 引用判等恢复：BServiceInfo.equals按name+identity，值判等无法区分并发写入的
				// 新版本对象；仅当映射仍是本次写入的实例才回滚，绝不吞并发edit的变更。
				// 回滚用compute原子完成"判等+修改"：get判等+remove/put两步的话，
				// 窗口内并发写入的equals相等新版本对象会被误删/误覆盖（remove按equals匹配键）。
				for (var reg : added)
					registers.compute(reg, (k, v) -> v == reg ? null : v);
				for (var e : replaced)
					registers.compute(e.getKey(), (k, v) -> v == e.getKey() ? e.getValue() : v);
				for (var e : removed)
					registers.putIfAbsent(e.getValue(), e.getValue()); // key==value同实例，维持registers不变式
				throw ex;
			}
		}
	}

	@Override
	public SubscribeState subscribeService(BSubscribeInfo info) {
		waitLoginReady();
		return super.subscribeService(info);
	}

	@Override
	public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(BSubscribeArgument arg) {
		waitLoginReady();
		logger.debug("subscribeServicesAsync: {}", arg);
		var cf = new CompletableFuture<List<SubscribeState>>();
		var r = new Subscribe(arg);
		raftClient.send(r, __ -> {
			var rc = r.getResultCode();
			if (rc == 0) {
				var edits = new BEditService();
				var states = new ArrayList<SubscribeState>(r.Argument.subs.size());
				for (var info : r.Argument.subs) {
					var state = subscribeStates.computeIfAbsent(info.getServiceName(), ___ -> new SubscribeState(info));
					state.updateSubscribeInfo(info); // 同名重订阅同步过滤版本，防重连重放回退
					states.add(state);
					var result = r.Result.map.get(info.getServiceName());
					if (result != null)
						state.onFirstCommit(result, edits);
				}
				try {
					triggerOnChanged(edits);
				} catch (Throwable e) {
					logger.error("subscribeServicesAsync: triggerOnChanged exception:", e);
				}
				cf.complete(states);
			} else {
				logger.error("subscribeServicesAsync: resultCode={}", rc);
				cf.completeExceptionally(new IllegalStateException("Subscribe resultCode=" + rc));
			}
			return 0;
		});
		return cf;
	}

	@Override
	public void unSubscribeService(BUnSubscribeArgument arg) {
		waitLoginReady();
		logger.debug("UnSubscribeService {}", arg);
		var r = new UnSubscribe(arg);
		raftClient.sendForWait(r).await();
		checkResultCode(r); // 服务端退订失败时不得移除本地subscribeStates，否则两侧状态分叉。
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		raftClient.send(new SetServerLoad(load), p -> 0);
		return true;
	}

	@Override
	public void close() {
		replay.close();
		try {
			loginFuture.cancel(true);
			raftClient.stop();
		} catch (Throwable e) {
			throw Task.forceThrow(e);
		}
	}

	@Override
	public void start() throws Exception {
		//noinspection DataFlowIssue
		raftClient.getClient().start();
	}

	@Override
	public void waitReady() {
		waitLoginReady();
	}
}
