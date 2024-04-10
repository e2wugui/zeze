package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Net.Service.ProtocolFactoryHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Agent extends AbstractAgent {
	static final Logger logger = LogManager.getLogger(Agent.class);

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public static final String defaultServiceName = "Zeze.Services.ServiceManager.Agent";

	private final AgentClient client;
	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();

	private Threading threading;

	public AgentClient getClient() {
		return client;
	}

	@Override
	public void start() throws Exception {
		client.start();
	}

	@Override
	public void waitReady() {
		waitConnectorReady();
	}

	public void waitConnectorReady() {
		// 实际上只有一个连接，这样就不用查找了。
		client.getConfig().forEachConnector(Connector::WaitReady);
	}

	@Override
	public BServiceInfo updateService(BServiceInfo info) {
		waitConnectorReady();
		var reg = registers.get(info);
		if (reg == null)
			return null;

		new Update(info).SendAndWaitCheckResultCode(client.getSocket());

		reg.setPassiveIp(info.getPassiveIp());
		reg.setPassivePort(info.getPassivePort());
		reg.setExtraInfo(info.getExtraInfo());
		return reg;
	}

	@Override
	public BServiceInfo registerService(BServiceInfo info) {
		verify(info.getServiceIdentity());
		waitConnectorReady();

		var regNew = new OutObject<Boolean>();
		regNew.value = false;
		var regServInfo = registers.computeIfAbsent(info, key -> {
			regNew.value = true;
			return key;
		});

		if (regNew.value) {
			try {
				new Register(info).SendAndWaitCheckResultCode(client.getSocket());
				logger.debug("RegisterService {}", info);
			} catch (Throwable e) { // rethrow
				// rollback.
				registers.remove(info, info); // rollback
				throw e;
			}
		}
		return regServInfo;
	}

	@Override
	public void unRegisterService(BServiceInfo info) {
		waitConnectorReady();

		var exist = registers.remove(info);
		if (exist != null) {
			try {
				new UnRegister(info).SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable e) { // rethrow
				// rollback.
				registers.putIfAbsent(exist, exist); // rollback
				throw e;
			}
		}
	}

	@Override
	public SubscribeState subscribeService(BSubscribeInfo info) {
		waitConnectorReady();

		final var newAdd = new OutObject<Boolean>();
		newAdd.value = false;
		var subState = subscribeStates.computeIfAbsent(info.getServiceName(), __ -> {
			newAdd.value = true;
			return new SubscribeState(info);
		});

		if (newAdd.value) {
			try {
				var r = new Subscribe(info);
				r.SendAndWaitCheckResultCode(client.getSocket());
				logger.debug("SubscribeService {}", info);
			} catch (Throwable ex) { // rethrow
				// rollback.
				subscribeStates.remove(info.getServiceName()); // rollback
				throw ex;
			}
		}
		return subState;
	}

	@Override
	public void unSubscribeService(String serviceName) {
		waitConnectorReady();

		var state = subscribeStates.remove(serviceName);
		if (state != null) {
			try {
				var r = new UnSubscribe(state.subscribeInfo);
				r.SendAndWaitCheckResultCode(client.getSocket());
				logger.debug("UnSubscribeService {}", state.subscribeInfo);
			} catch (Throwable e) { // rethrow
				// rollback.
				subscribeStates.putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	@Override
	public void offlineRegister(BOfflineNotify argument, Action1<BOfflineNotify> handle) {
		waitConnectorReady();
		onOfflineNotifies.putIfAbsent(argument.notifyId, handle);
		new OfflineRegister(argument).SendAndWaitCheckResultCode(client.getSocket());
	}

	@Override
	public boolean setServerLoad(BServerLoad load) {
		return new SetServerLoad(load).Send(client.getSocket());
	}

	@Override
	protected void allocate(AutoKey autoKey, int pool) {
		if (pool < 1)
			throw new IllegalArgumentException();
		var r = new AllocateId();
		r.Argument.setName(autoKey.getName());
		r.Argument.setCount(pool);
		r.SendAndWaitCheckResultCode(client.getSocket());
		autoKey.setCurrentAndCount(r.Result.getStartId(), r.Result.getCount());
	}

	@Override
	protected boolean sendReadyList(String serviceName, long serialId) {
		var r = new ReadyServiceList();
		r.Argument.serviceName = serviceName;
		r.Argument.serialId = serialId;
		var s = client.getSocket();
		return s != null && s.Send(r);
	}

	public void onConnected() {
		for (var e : registers.keySet()) {
			try {
				new Register(e).SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable ex) { // logger.debug
				// skip and continue.
				logger.debug("OnConnected.Register={}", e, ex);
			}
		}
		for (var e : subscribeStates.values()) {
			try {
				e.committed = false;
				var r = new Subscribe();
				r.Argument = e.subscribeInfo;
				r.SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable ex) { // logger.debug
				// skip and continue.
				logger.debug("OnConnected.Subscribe={}", e.subscribeInfo, ex);
			}
		}
	}

	private long processRegister(Register r) {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	private long processUnRegister(UnRegister r) {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUnRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	private long processUpdate(Update r) {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;
		state.onUpdate(r.Argument);
		r.SendResult();
		return 0;
	}

	private long processNotifyServiceList(NotifyServiceList r) {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onNotify(r.Argument);
		else
			logger.warn("NotifyServiceList But SubscribeState Not Found.");
		return Procedure.Success;
	}

	private long processSubscribeFirstCommit(SubscribeFirstCommit r) {
		var state = subscribeStates.get(r.Argument.getServiceName());
		if (state != null)
			state.onFirstCommit(r.Argument);
		return Procedure.Success;
	}

	private long processCommitServiceList(CommitServiceList r) {
		var state = subscribeStates.get(r.Argument.serviceName);
		if (state != null)
			state.onCommit(r.Argument);
		else
			logger.warn("CommitServiceList But SubscribeState Not Found.");
		return Procedure.Success;
	}

	private long processKeepAlive(KeepAlive r) {
		if (onKeepAlive != null)
			Task.getCriticalThreadPool().execute(onKeepAlive);
		r.SendResultCode(KeepAlive.Success);
		return Procedure.Success;
	}

	private long processSetServerLoad(SetServerLoad setServerLoad) {
		loads.put(setServerLoad.Argument.getName(), setServerLoad.Argument);
		if (onSetServerLoad != null) {
			Task.getCriticalThreadPool().execute(() -> {
				try {
					onSetServerLoad.run(setServerLoad.Argument);
				} catch (Throwable e) { // logger.error
					// run handle.
					logger.error("", e);
				}
			});
		}
		return Procedure.Success;
	}

	private long processOfflineNotify(OfflineNotify r) {
		try {
			if (triggerOfflineNotify(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) { // ignored
			// rpc response any.
			r.trySendResultCode(3);
		}
		return 0;
	}

	public Agent(Config config) {
		this(config, null);
	}

	public Agent(Config config, String netServiceName) {
		super.config = config;

		client = (null == netServiceName || netServiceName.isEmpty())
				? new AgentClient(this, config)
				: new AgentClient(this, config, netServiceName);

		client.AddFactoryHandle(Register.TypeId_, new ProtocolFactoryHandle<>(
				Register::new, this::processRegister, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(UnRegister.TypeId_, new ProtocolFactoryHandle<>(
				UnRegister::new, this::processUnRegister, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(Update.TypeId_, new ProtocolFactoryHandle<>(
				Update::new, this::processUpdate, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
				Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(UnSubscribe.TypeId_, new ProtocolFactoryHandle<>(
				UnSubscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(NotifyServiceList.TypeId_, new ProtocolFactoryHandle<>(
				NotifyServiceList::new, this::processNotifyServiceList, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(SubscribeFirstCommit.TypeId_, new ProtocolFactoryHandle<>(
				SubscribeFirstCommit::new, this::processSubscribeFirstCommit, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(CommitServiceList.TypeId_, new ProtocolFactoryHandle<>(
				CommitServiceList::new, this::processCommitServiceList, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
				KeepAlive::new, this::processKeepAlive, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
				AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		client.AddFactoryHandle(SetServerLoad.TypeId_, new ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetServerLoad, TransactionLevel.None, DispatchMode.Direct));

		client.AddFactoryHandle(OfflineNotify.TypeId_, new ProtocolFactoryHandle<>(
				OfflineNotify::new, this::processOfflineNotify, TransactionLevel.None, DispatchMode.Critical));
		client.AddFactoryHandle(OfflineRegister.TypeId_, new ProtocolFactoryHandle<>(
				OfflineRegister::new, null, TransactionLevel.None, DispatchMode.Normal));
		client.AddFactoryHandle(NormalClose.TypeId_, new ProtocolFactoryHandle<>(
				NormalClose::new, null, TransactionLevel.None, DispatchMode.Critical));

		threading = new Threading(client, config.getServerId());
		threading.RegisterProtocols(client);
	}

	@Override
	public Threading getThreading() {
		return threading;
	}

	public void stop() throws Exception {
		lock();
		try {
			if (client != null) {
				var so = client.getSocket();
				if (so != null) // 有可能提前关闭,so==null时执行下面这行会抛异常
					new NormalClose().SendAndWaitCheckResultCode(so);
				client.stop();
			}
			if (null != threading) {
				threading.close();
				threading = null;
			}
		} finally {
			unlock();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
