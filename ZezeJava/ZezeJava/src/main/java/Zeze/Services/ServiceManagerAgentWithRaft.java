package Zeze.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.NormalClose;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
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
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Agent raftClient;

	@Override
	public Threading getThreading() {
		throw new UnsupportedOperationException();
	}

	public ServiceManagerAgentWithRaft(@NotNull Config config) throws Exception {
		super.config = config;

		var raftConf = RaftConfig.load(config.getServiceManagerConf().getRaftXml());
		raftClient = new Agent("servicemanager.raft", raftConf, config);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		raftClient.dispatchProtocolToInternalThreadPool = true;
		RegisterProtocols(raftClient.getClient());
	}

	private void raftOnSetLeader(Agent agent) {
		var client = agent.getClient();
		if (client == null)
			return;
		var zeze = client.getZeze();
		if (zeze == null)
			return;
		var config = zeze.getConfig();

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
			}
			return 0;
		});
	}

	////////////////////////////////////////////////////////////////////////
	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessOfflineNotifyRequest(OfflineNotify r) throws Exception {
		try {
			if (triggerOfflineNotify(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) { // ignored
			r.trySendResultCode(3);
		}
		return 0;
	}

	@Override
	protected long ProcessEditRequest(Edit r) {
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
	protected long ProcessSetServerLoadRequest(SetServerLoad r) throws Exception {
		loads.put(r.Argument.getName(), r.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					onSetServerLoad.run(r.Argument);
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				}
			});
		}
		r.SendResult();
		return 0;
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
				Task.forceThrow(ex);
			}
			return 0;
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
		if (r.getResultCode() == 0) // setCurrentAndCount is in super.
			setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();

	private void waitLoginReady() {
		var volatileTmp = loginFuture;
		if (volatileTmp.isDone()) {
			if (volatileTmp.get())
				return;
			throw new IllegalStateException("login fail.");
		}
		if (!volatileTmp.await(super.config.getServiceManagerConf().getLoginTimeout()))
			throw new IllegalStateException("login timeout.");
		// 再次查看结果。
		if (volatileTmp.isDone() && volatileTmp.get())
			return;
		// 只等待一次，不成功则失败。
		throw new IllegalStateException("login timeout.");
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

	@Override
	public void editService(@NotNull BEditService arg) {
		for (var info : arg.getAdd())
			verify(info.getServiceIdentity());
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
	public CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument arg) {
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
	public void offlineRegister(@NotNull BOfflineNotify argument, @NotNull Action1<BOfflineNotify> handle) {
		waitLoginReady();
		onOfflineNotifies.putIfAbsent(argument.notifyId, handle);
		raftClient.sendForWait(new OfflineRegister(argument)).await();
	}

	@Override
	public void close() {
		try {
			var tmp = loginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			raftClient.sendForWait(new NormalClose()).await();
			raftClient.stop();
		} catch (Throwable e) { // rethrow RuntimeException
			Task.forceThrow(e);
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
