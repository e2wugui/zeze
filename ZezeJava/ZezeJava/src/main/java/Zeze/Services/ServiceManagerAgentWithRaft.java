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
import Zeze.Net.RpcTimeoutException;
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
import org.jspecify.annotations.Nullable;

/**
 * Raft版服务管理器客户端Agent：经raft连接ServiceManagerWithRaft，注册/订阅断线重连后
 * 自动重放，不支持Id128 UDP发号（History组合构造即fail-fast）。
 */
public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	private static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Agent raftClient;
	private volatile boolean closed;
	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	// 断线或换主重连后重放已注册服务，恢复服务端可能被 onClose 清理的状态
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	@Override
	public Threading getThreading() {
		throw new UnsupportedOperationException();
	}

	public ServiceManagerAgentWithRaft(Config config) throws Exception {
		// Raft 版不支持 Id128 UDP 发号，开启 History 时构造期立即拒绝
		if (config.isHistory())
			throw new IllegalStateException("ServiceManager=raft does not support Id128 allocate: " +
					"History('" + config.getHistory() + "') requires it. " +
					"Use a non-raft ServiceManager or disable History.");
		// 禁止空白 sessionName，避免多个未配置节点共享同一会话行互相接管
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
	}

	private void raftOnSetLeader(Agent agent) {
		if (closed)
			return;
		// raftClient 由 Config 直接构造（无 Application），需直接读取自身 config
		var future = startNewLogin();
		if (future == null)
			return;
		var login = new Login();
		//noinspection DataFlowIssue
		login.Argument.setSessionName(config.getServiceManagerConf().getSessionName());

		agent.send(login, p -> {
			if (closed)
				return 0;
			var rpc = (Login)p;
			if (rpc.isTimeout()) {
				if (future == loginFuture)
					raftOnSetLeader(agent);
			} else if (rpc.getResultCode() != 0 && rpc.getResultCode() != Procedure.RaftApplied) {
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
		// 先上报 Identify 让服务端记录 serverId，便于断线时广播 Suspect 提示
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
			//noinspection DataFlowIssue
			Task.getCriticalThreadPool().execute(onKeepAlive);
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

	// Direct 派发：与 Subscribe 应答同在 IO 线程按 TCP 接收序串行应用，防止无序号增量乱序分叉
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
			//noinspection DataFlowIssue
			Task.getCriticalThreadPool().execute(() -> {
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
		// 显式检查返回码：失败应答携带的是服务端回滚前数据，不可使用
		checkResultCode(r);
		setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	/**
	 * 检查 RPC 返回码（仅 0 或 RaftApplied 视为成功），防止错误应答被当作成功导致本地与服务端状态分叉。
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
			if (closed)
				throw new IllegalStateException("ServiceManagerAgentWithRaft closed");
			var volatileTmp = loginFuture;
			// 仅在已完成或 await 返回 true 时调用 get()，避免超时未完成时无限期阻塞
			if (volatileTmp.isDone() || volatileTmp.await(Math.max(1, deadline - System.currentTimeMillis()))) {
				if (closed)
					throw new IllegalStateException("ServiceManagerAgentWithRaft closed");
				try {
					if (Boolean.TRUE.equals(volatileTmp.get()))
						return;
				} catch (Throwable ignored) {
					// 换主期间旧 future 会被新 loginFuture 替换取消，重读最新 future 继续等
					if (volatileTmp == loginFuture)
						throw new IllegalStateException("login canceled.");
				}
			}
			if (closed)
				throw new IllegalStateException("ServiceManagerAgentWithRaft closed");
			if (System.currentTimeMillis() >= deadline)
				throw new IllegalStateException("login timeout.");
		}
	}

	private @Nullable TaskCompletionSource<Boolean> startNewLogin() {
		lock();
		try {
			var old = loginFuture;
			if (closed) {
				old.cancel(true);
				return null;
			}
			var next = new TaskCompletionSource<Boolean>();
			loginFuture = next;
			old.cancel(true); // 先发布新future再取消旧future，避免等待者短暂重读已取消的旧future。
			return next;
		} finally {
			unlock();
		}
	}

	// 注册确认和重连重放共用串行边界；本地仅发布已确认状态，失败无需逐项回滚。
	private final Object editServiceLock = new Object();

	@Override
	public void editService(BEditService arg) {
		synchronized (editServiceLock) {
			for (var info : arg.getAdd())
				verify(info.getServiceIdentity());
			waitLoginReady();

			var edit = new Edit(arg);
			raftClient.sendForWait(edit).await();
			checkResultCode(edit);
			for (var unReg : arg.getRemove())
				registers.remove(unReg);
			for (var reg : arg.getAdd())
				registers.put(reg, reg);
			logger.debug("EditService {}", arg);
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
			if (rc == 0 || rc == Procedure.RaftApplied) {
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
		var done = new TaskCompletionSource<Boolean>();
		raftClient.send(r, __ -> {
			try {
				if (r.isTimeout())
					throw RpcTimeoutException.getInstance();
				// 应答在 IO 线程串行处理，先应用退订再完成等待，不能让旧调用者删除新订阅。
				if (r.getResultCode() == 0 || r.getResultCode() == Procedure.RaftApplied)
					applyUnsubscribe(arg);
				done.setResult(true);
			} catch (Throwable e) {
				done.setException(e);
			}
			return 0;
		});
		done.await();
		checkResultCode(r); // 维持调用者直接收到 IllegalStateException 的原有错误码契约。
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		raftClient.send(new SetServerLoad(load), p -> 0);
		return true;
	}

	@Override
	public void close() {
		closed = true;
		replay.close();
		try {
			lock();
			try {
				loginFuture.cancel(true);
			} finally {
				unlock();
			}
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
