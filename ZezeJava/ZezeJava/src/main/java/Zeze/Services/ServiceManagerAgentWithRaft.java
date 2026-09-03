package Zeze.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.Identify;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Suspect;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Edit;
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
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	private static final @NotNull Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final @NotNull Agent raftClient;
	private volatile @NotNull TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();
	// 断线（换leader）重连后重放用：服务端行按name持久化，但flap时行可能已被onClose删除，
	// 重放注册/订阅才能恢复服务端状态（对齐非raft版Agent.onConnected）。
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	@Override
	public @NotNull Threading getThreading() {
		throw new UnsupportedOperationException();
	}

	public ServiceManagerAgentWithRaft(@NotNull Config config) throws Exception {
		// raft版不支持Id128 UDP发号（tid128UdpClient不初始化），而开启History的事务在
		// _check_预热/finalCommit（getUsableTid128CacheFuture）/Tid128Cache.next三个入口
		// 都依赖它：该组合下写事务全量NPE失败、热事务finalCommit失败直接halt(543543)。
		// 不支持的组合在构造时明确报错（fail-fast），而非运行期以NPE/halt形态失败。
		if (config.isHistory())
			throw new IllegalStateException("ServiceManager=raft does not support Id128 allocate: " +
					"History('" + config.getHistory() + "') requires it. " +
					"Use a non-raft ServiceManager or disable History.");
		super.config = config;

		var raftConf = RaftConfig.load(config.getServiceManagerConf().getRaftXml());
		raftClient = new Agent("servicemanager.raft", raftConf, config);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		raftClient.dispatchProtocolToInternalThreadPool = true;
		RegisterProtocols(raftClient.getClient());

		// todo raft版本先不支持Id128分配了.
		// super.tid128UdpClient = new Id128UdpClient(0, raftClient.getClient());
	}

	private void raftOnSetLeader(@NotNull Agent agent) {
		// 直接使用自身持有的config。raftClient以Config构造（无Application），getClient().getZeze()为null，
		// 原来经zeze round-trip取配置会在null检查处直接return，Login永远不发送。
		var future = startNewLogin();
		var login = new Login();
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
				TaskSpec.ofAction(this::onLoginSuccess).name("ServiceManager.AgentWithRaft.OnLoginSuccess").run();
			}
			return 0;
		});
	}

	/**
	 * 每次Login成功后执行（含断线重连/换leader）：上报Identify、重放全部注册、订阅，
	 * 恢复服务端状态。服务端幂等（AddOrUpdate、允许重复注册），重复重放无害。
	 */
	private void onLoginSuccess() {
		// 先上报Identify：SM据此把serverId记在会话上，断线时广播Suspect（对齐非raft版onConnected）。
		// 重发=恢复提示资格（与正确性无关，正确性由Takeover租约裁决）。
		try {
			var identify = new Identify();
			identify.Argument.serverId = config.getServerId();
			raftClient.send(identify, __ -> 0L);
		} catch (Throwable ex) { // logger.error
			logger.error("OnLoginSuccess.Identify", ex);
		}

		var edit = new BEditService();
		edit.getAdd().addAll(registers.keySet());
		if (!edit.getAdd().isEmpty()) {
			try {
				editService(edit);
			} catch (Throwable ex) { // logger.error
				logger.error("OnLoginSuccess.Register", ex);
			}
		}

		var subArg = new BSubscribeArgument();
		for (var e : subscribeStates.values())
			subArg.subs.add(e.getSubscribeInfo());
		if (!subArg.subs.isEmpty()) {
			try {
				subscribeServicesAsync(subArg);
			} catch (Throwable ex) { // logger.error
				logger.error("OnLoginSuccess.Subscribe", ex);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////
	@Override
	protected long ProcessKeepAliveRequest(@NotNull KeepAlive r) {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	// Suspect仅是提示：转化为onSuspect回调（应用接takeover.tryTransfer），
	// 租约未过期时tryTransfer内部安排到过期时刻精确重试，不会误接管。
	@Override
	protected long ProcessSuspectRequest(@NotNull Suspect r) throws Exception {
		var on = onSuspect;
		if (on != null) {
			try {
				on.run(r.Argument.serverId);
			} catch (Throwable e) { // logger.error
				logger.error("ProcessSuspectRequest serverId=" + r.Argument.serverId, e);
			}
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessEditRequest(@NotNull Edit r) {
		for (var it = r.Argument.getRemove().iterator(); it.hasNext(); /**/) {
			var unReg = it.next();
			var state = subscribeStates.get(unReg.getServiceName());
			if (null == state || !state.onUnRegister(unReg))
				it.remove();
		}

		// 触发回调前修正集合之间的关系。
		// 删除后来又加入的。
		r.Argument.getRemove().removeIf(r.Argument.getAdd()::contains);

		for (var reg : r.Argument.getAdd()) {
			var state = subscribeStates.get(reg.getServiceName());
			if (null == state)
				continue; // 忽略本地没有订阅的。最好加个日志。
			var oldNotSame = state.onRegister(reg);
			if (null != oldNotSame)
				r.Argument.getRemove().add(oldNotSame);
		}

		r.SendResult();
		try {
			triggerOnChanged(r.Argument);
		} catch (Throwable e) { // logger.error
			logger.error("ProcessEditRequest: triggerOnChanged exception:", e);
		}
		return 0;
	}

	@Override
	protected long ProcessSetServerLoadRequest(@NotNull SetServerLoad r) {
		loads.put(r.Argument.getName(), r.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					var onSetLoad = onSetServerLoad;
					if (onSetLoad != null){
						onSetLoad.run(r.Argument);
					}
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				}
			});
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected boolean allocateAsync(@NotNull String globalName, int allocCount,
									@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
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
	protected void allocate(@NotNull AutoKey autoKey, int pool) {
		if (pool < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(pool);
		raftClient.sendForWait(r).await();
		if (r.getResultCode() == 0) // setCurrentAndCount is in super.
			setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	private void waitLoginReady() {
		var deadline = System.currentTimeMillis() + super.config.getServiceManagerConf().getLoginTimeout();
		for (; ; ) {
			var volatileTmp = loginFuture;
			// await超时或被取消都返回false，此时不能再调get()：未完成的future上get()会无限期park，
			// 下面的deadline检查将不可达；被取消则重读最新loginFuture继续等。
			if (volatileTmp.isDone() || volatileTmp.await(Math.max(1, deadline - System.currentTimeMillis()))) {
				try {
					if (volatileTmp.get()) // 到这里future已完成，get()不会park。
						return;
				} catch (Throwable ignored) { // ignored
					// 等待期间raftOnSetLeader执行startNewLogin，cancel旧future并替换；
					// 被替换不是失败，重读最新future继续等。
				}
			}
			if (System.currentTimeMillis() >= deadline)
				throw new IllegalStateException("login timeout.");
		}
	}

	private @NotNull TaskCompletionSource<Boolean> startNewLogin() {
		lock();
		try {
			loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
			return loginFuture = new TaskCompletionSource<>();
		} finally {
			unlock();
		}
	}

	@Override
	public void editService(@NotNull BEditService arg) {
		for (var info : arg.getAdd())
			verify(info.getServiceIdentity());
		// 先更新本地记录再发送远程请求（重连重放的数据来源）
		for (var unReg : arg.getRemove())
			registers.remove(unReg);
		for (var reg : arg.getAdd())
			registers.put(reg, reg);
		waitLoginReady();

		var edit = new Edit(arg);
		raftClient.sendForWait(edit).await();
		logger.debug("EditService {}", arg);
	}

	@Override
	public @NotNull SubscribeState subscribeService(@NotNull BSubscribeInfo info) {
		waitLoginReady();
		return super.subscribeService(info);
	}

	@Override
	public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument arg) {
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
					states.add(state);
					var result = r.Result.map.get(info.getServiceName());
					if (result != null)
						state.onFirstCommit(result, edits);
				}
				try {
					triggerOnChanged(edits);
				} catch (Throwable e) { // logger.error
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
	public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
		waitLoginReady();
		logger.debug("UnSubscribeService {}", arg);
		var r = new UnSubscribe(arg);
		raftClient.sendForWait(r).await();
		for (var serviceName : arg.serviceNames)
			subscribeStates.remove(serviceName);
	}

	@Override
	public boolean setServerLoad(@NotNull BServerLoad load) {
		raftClient.send(new SetServerLoad(load), p -> 0);
		return true;
	}

	@Override
	public void close() {
		try {
			loginFuture.cancel(true);
			raftClient.stop();
		} catch (Throwable e) { // rethrow
			throw Task.forceThrow(e);
		}
	}

	@Override
	public void start() throws Exception {
		raftClient.getClient().start();
	}

	@Override
	public void waitReady() {
		waitLoginReady();
	}
}
