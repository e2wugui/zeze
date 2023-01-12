package Zeze.Services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import Zeze.Builtin.ServiceManagerWithRaft.AllocateId;
import Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.KeepAlive;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify;
import Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister;
import Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList;
import Zeze.Builtin.ServiceManagerWithRaft.Register;
import Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad;
import Zeze.Builtin.ServiceManagerWithRaft.Subscribe;
import Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit;
import Zeze.Builtin.ServiceManagerWithRaft.UnRegister;
import Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe;
import Zeze.Builtin.ServiceManagerWithRaft.Update;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Zeze.Raft.Agent raftClient;

	public ServiceManagerAgentWithRaft(Zeze.Application zeze) throws Throwable {
		super.zeze = zeze;

		var config = zeze.getConfig();
		if (null == config) {
			throw new IllegalStateException("Config is null");
		}

		var raftConf = Zeze.Raft.RaftConfig.load(config.getServiceManagerConf().getRaftXml());
		raftClient = new Zeze.Raft.Agent("servicemanager.raft", zeze, raftConf);
		raftClient.setOnSetLeader(this::raftOnSetLeader);
		raftClient.dispatchProtocolToInternalThreadPool = true;
		RegisterProtocols(raftClient.getClient());
	}

	private void raftOnSetLeader(Zeze.Raft.Agent agent) {
		var client = agent.getClient();
		if (client == null)
			return;
		var zeze = client.getZeze();
		if (zeze == null)
			return;
		var config = zeze.getConfig();
		if (config == null)
			return;

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
		}, true);
	}

	////////////////////////////////////////////////////////////////////////
	@Override
	protected long ProcessCommitServiceListRequest(CommitServiceList r) throws Throwable {
		var state = subscribeStates.get(r.Argument.serviceName);
		if (state != null)
			state.onCommit(r.Argument);
		else
			logger.warn("CommitServiceList But SubscribeState Not Found.");
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Throwable {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessNotifyServiceListRequest(NotifyServiceList r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onNotify(r.Argument);
		else
			logger.warn("NotifyServiceList But SubscribeState Not Found.");
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessOfflineNotifyRequest(OfflineNotify r) throws Throwable {
		if (onOfflineNotify == null) {
			r.trySendResultCode(1);
			return 0;
		}
		try {
			if (onOfflineNotify.call(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) {
			r.trySendResultCode(3);
		}
		return 0;
	}

	@Override
	protected long ProcessRegisterRequest(Register r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) throws Throwable {
		loads.put(r.Argument.getName(), r.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					onSetServerLoad.run(r.Argument);
				} catch (Throwable e) {
					logger.error("", e);
				}
			});
		}
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSubscribeFirstCommitRequest(SubscribeFirstCommit r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onFirstCommit(r.Argument);
		r.SendResult();
		return Procedure.Success;
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUnRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUpdate(r.Argument);
		r.SendResult();
		return 0;
	}

	@Override
	protected boolean sendReadyList(String serviceName, long serialId) {
		var r = new ReadyServiceList();
		r.Argument.serviceName = serviceName;
		r.Argument.serialId = serialId;
		raftClient.send(r, p -> 0);
		return true;
	}

	@Override
	protected void allocate(AutoKey autoKey) {
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(1024);
		raftClient.sendForWait(r).await();
		if (r.getResultCode() == 0) // setCurrentAndCount is in super.
			setCurrentAndCount(autoKey, r.Result.getStartId(), r.Result.getCount());
	}

	private volatile TaskCompletionSource<Boolean> loginFuture = new TaskCompletionSource<>();

	private void waitLoginReady() {
		var volatileTmp = loginFuture;
		if (volatileTmp.isDone()) {
			try {
				if (volatileTmp.get())
					return;
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			throw new IllegalStateException("login fail.");
		}
		if (!volatileTmp.await(super.zeze.getConfig().getServiceManagerConf().getLoginTimeout()))
			throw new IllegalStateException("login timeout.");
		// 再次查看结果。
		try {
			if (volatileTmp.isDone() && volatileTmp.get())
				return;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		// 只等待一次，不成功则失败。
		throw new IllegalStateException("login timeout.");
	}

	private synchronized TaskCompletionSource<Boolean> startNewLogin() {
		loginFuture.cancel(true); // 如果旧的Future上面有人在等，让他们失败。
		return loginFuture = new TaskCompletionSource<>();
	}

	@Override
	public BServiceInfo registerService(BServiceInfo info) {
		verify(info.getServiceIdentity());
		waitLoginReady();
		raftClient.sendForWait(new Register(info)).await();
		logger.debug("RegisterService {}", info);
		return info;
	}

	@Override
	public BServiceInfo updateService(BServiceInfo info) {
		waitLoginReady();
		raftClient.sendForWait(new Update(info)).await();
		return info;
	}

	@Override
	public void unRegisterService(BServiceInfo info) {
		waitLoginReady();
		raftClient.sendForWait(new UnRegister(info)).await();
	}

	@Override
	public SubscribeState subscribeService(BSubscribeInfo info) {
		waitLoginReady();

		final var newAdd = new OutObject<Boolean>();
		newAdd.value = false;
		var subState = subscribeStates.computeIfAbsent(info.getServiceName(), __ -> {
			newAdd.value = true;
			return new SubscribeState(info);
		});

		if (newAdd.value) {
			try {
				raftClient.sendForWait(new Subscribe(info)).await();
				logger.debug("SubscribeService {}", info);
			} catch (Throwable ex) {
				// 【警告】这里没有原子化执行请求和处理结果。
				// 由于上面是computeIfAbsent，仅第一个请求会发送，不会并发发送相同的订阅，所以，
				// 可以在这里rollback处理一下。
				subscribeStates.remove(info.getServiceName()); // rollback
				throw ex;
			}
		}
		return subState;
	}

	@Override
	public void unSubscribeService(String serviceName) {
		waitLoginReady();

		var state = subscribeStates.remove(serviceName);
		if (state != null) {
			try {
				raftClient.sendForWait(new UnSubscribe(state.subscribeInfo)).await();
				logger.debug("UnSubscribeService {}", state.subscribeInfo);
			} catch (Throwable e) {
				subscribeStates.putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		raftClient.send(new SetServerLoad(load), p -> 0);
		return true;
	}

	@Override
	public void offlineRegister(BOfflineNotify argument) {
		waitLoginReady();
		raftClient.sendForWait(new OfflineRegister(argument)).await();
	}

	@Override
	public void close() throws IOException {
		try {
			var tmp = loginFuture;
			if (null != tmp) {
				tmp.cancel(true);
			}
			raftClient.stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void start() throws Throwable {
		raftClient.getClient().start();
	}

	@Override
	public void waitReady() {
		waitLoginReady();
	}
}
