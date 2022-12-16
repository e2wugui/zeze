package Zeze.Services;

import java.io.IOException;
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
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BServiceListVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceManagerAgentWithRaft extends AbstractServiceManagerAgentWithRaft {
	static final Logger logger = LogManager.getLogger(ServiceManagerAgentWithRaft.class);
	private final Zeze.Raft.Agent raftClient;
	private final String sessionName;

	public ServiceManagerAgentWithRaft(String sessionName, Zeze.Application zeze, Zeze.Raft.RaftConfig raftConf) throws Throwable {
		this.sessionName = sessionName;
		super.zeze = zeze;

		var config = zeze.getConfig();
		if (null == config) {
			throw new IllegalStateException("Config is null");
		}

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

		var login = new Login();
		login.Argument.setSessionName(sessionName);

		agent.send(login, p -> {
			var rpc = (Login)p;
			if (rpc.isTimeout())
				raftOnSetLeader(agent);
			else if (rpc.getResultCode() != 0) {
				logger.error("Login Timeout Or ResultCode != 0. Code={}", rpc.getResultCode());
			}
			return 0;
		}, true);
	}

	////////////////////////////////////////////////////////////////////////
	private static BServiceListVersion fromRaft(Zeze.Builtin.ServiceManagerWithRaft.BServiceListVersion a) {
		var r = new BServiceListVersion();
		r.serviceName = a.getServiceName();
		r.serialId = a.getSerialId();
		return r;
	}

	@Override
	protected long ProcessCommitServiceListRequest(CommitServiceList r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onCommit(fromRaft(r.Argument));
		else
			logger.warn("CommitServiceList But SubscribeState Not Found.");
		return Procedure.Success;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive r) throws Throwable {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResult();
		return Procedure.Success;
	}

	private static BServiceInfo fromRaft(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo a) {
		var r = new BServiceInfo();
		r.serviceName = a.getServiceName();
		r.serviceIdentity = a.getServiceIdentity();
		r.setPassiveIp(a.getPassiveIp());
		r.setPassivePort(a.getPassivePort());
		r.setExtraInfo(a.getExtraInfo());
		return r;
	}

	private static BServiceInfos fromRaft(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfos a) {
		var r = new BServiceInfos();
		r.serviceName = a.getServiceName();
		r.serialId = a.getSerialId();
		for (var si : a.getServiceInfoListSortedByIdentity())
			r.serviceInfoListSortedByIdentity.add(fromRaft(si));
		return r;
	}

	@Override
	protected long ProcessNotifyServiceListRequest(NotifyServiceList r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onNotify(fromRaft(r.Argument));
		else
			logger.warn("NotifyServiceList But SubscribeState Not Found.");
		return Procedure.Success;
	}

	private static BOfflineNotify fromRaft(Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify a) {
		var r = new BOfflineNotify();
		r.serverId = a.getServerId();
		r.notifyId = a.getNotifyId();
		r.notifySerialId = a.getNotifySerialId();
		r.notifyContext = a.getNotifyContext();
		return r;
	}

	@Override
	protected long ProcessOfflineNotifyRequest(OfflineNotify r) throws Throwable {
		if (onOfflineNotify == null) {
			r.trySendResultCode(1);
			return 0;
		}
		try {
			if (onOfflineNotify.call(fromRaft(r.Argument))) {
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
		state.onRegister(fromRaft(r.Argument));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSubscribeFirstCommitRequest(SubscribeFirstCommit r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onFirstCommit(fromRaft(r.Argument));
		return Procedure.Success;
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUnRegister(fromRaft(r.Argument));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) throws Throwable {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUpdate(fromRaft(r.Argument));
		r.SendResult();
		return 0;
	}

	@Override
	protected boolean sendReadyList(String serviceName, long serialId) {
		var r = new ReadyServiceList();
		r.Argument.setServiceName(serviceName);
		r.Argument.setSerialId(serialId);

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

	private Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo toRaft(BServiceInfo a) {
		var r = new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo();
		r.setServiceName(a.serviceName);
		r.setServiceIdentity(a.serviceIdentity);
		r.setPassiveIp(a.getPassiveIp());
		r.setPassivePort(a.getPassivePort());
		r.setExtraInfo(a.getExtraInfo());
		return r;
	}

	private void waitLoginReady() {
		// raft onSetLeader是第一个就发送了Login，实际上不需要等待登录成功。
		// 写在这里，保留实现等待登录成功。
	}

	@Override
	public BServiceInfo registerService(BServiceInfo info) {
		verify(info.getServiceIdentity());
		waitLoginReady();
		raftClient.sendForWait(new Register(toRaft(info))).await();
		logger.debug("RegisterService {}", info);
		return info;
	}

	@Override
	public BServiceInfo updateService(BServiceInfo info) {
		waitLoginReady();
		raftClient.sendForWait(new Update(toRaft(info))).await();
		return info;
	}

	@Override
	public void unRegisterService(BServiceInfo info) {
		waitLoginReady();
		raftClient.sendForWait(new UnRegister(toRaft(info))).await();
	}

	private static Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo toRaft(BSubscribeInfo a) {
		var r = new Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo();
		r.setServiceName(a.getServiceName());
		r.setSubscribeType(a.getSubscribeType());
		return r;
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
			raftClient.sendForWait(new Subscribe(toRaft(info))).await();
			logger.debug("SubscribeService {}", info);
		}
		return subState;
	}

	@Override
	public void unSubscribeService(String serviceName) {
		waitLoginReady();

		var state = subscribeStates.remove(serviceName);
		if (state != null) {
			try {
				raftClient.sendForWait(new UnSubscribe(toRaft(state.subscribeInfo))).await();
				logger.debug("UnSubscribeService {}", state.subscribeInfo);
			} catch (Throwable e) {
				subscribeStates.putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	private Zeze.Builtin.ServiceManagerWithRaft.BServerLoad toRaft(BServerLoad a) {
		var r = new Zeze.Builtin.ServiceManagerWithRaft.BServerLoad();
		r.setIp(a.ip);
		r.setPort(a.port);
		r.setParam(a.param);
		return r;
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		raftClient.send(new SetServerLoad(toRaft(load)), p -> 0);
		return true;
	}

	private static Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify toRaft(BOfflineNotify a) {
		var r = new Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify();
		r.setServerId(a.serverId);
		r.setNotifyId(a.notifyId);
		r.setNotifySerialId(a.notifySerialId);
		r.setNotifyContext(a.notifyContext);
		return r;
	}

	@Override
	public void offlineRegister(BOfflineNotify argument) {
		waitLoginReady();
		raftClient.sendForWait(new OfflineRegister(toRaft(argument))).await();
	}

	@Override
	public void close() throws IOException {
		try {
			raftClient.stop();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
