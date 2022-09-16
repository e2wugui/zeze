package Zeze.Services.ServiceManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service.ProtocolFactoryHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action1;
import Zeze.Util.Action2;
import Zeze.Util.Func1;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Agent implements Closeable {
	static final Logger logger = LogManager.getLogger(Agent.class);

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public static final String defaultServiceName = "Zeze.Services.ServiceManager.Agent";

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	private final ConcurrentHashMap<String, SubscribeState> subscribeStates = new ConcurrentHashMap<>();
	private AgentClient client;
	private final Zeze.Application zeze;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	private Action1<SubscribeState> onChanged; // Simple (如果没有定义OnUpdate和OnRemove) Or ReadyCommit (Notify, Commit)
	private Action2<SubscribeState, BServiceInfo> onUpdate; // Simple (Register, Update)
	private Action2<SubscribeState, BServiceInfo> onRemove; // Simple (UnRegister)
	private Action1<SubscribeState> onPrepare; // ReadyCommit 的第一步回调。
	private Action1<BServerLoad> onSetServerLoad;
	private Func1<BOfflineNotify, Boolean> onOfflineNotify; // 返回是否处理成功且不需要其它notifier继续处理

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	private Runnable onKeepAlive;

	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> registers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();

	public final ConcurrentHashMap<String, BServerLoad> loads = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
		return subscribeStates;
	}

	/*
	public Object getLocalState(String serviceName, String identity) {
		return SubscribeStates.get(serviceName).LocalStates.get(identity);
	}

	public Object getLocalState(ServiceInfo serviceInfo) {
		return getLocalState(serviceInfo.getServiceName(), serviceInfo.getServiceIdentity());
	}
	*/

	public AgentClient getClient() {
		return client;
	}

	public Zeze.Application getZeze() {
		return zeze;
	}

	public Action1<SubscribeState> getOnChanged() {
		return onChanged;
	}

	public void setOnChanged(Action1<SubscribeState> value) {
		onChanged = value;
	}

	public void setOnSetServerLoad(Action1<BServerLoad> value) {
		onSetServerLoad = value;
	}

	public void setOnOfflineNotify(Func1<BOfflineNotify, Boolean> value) {
		onOfflineNotify = value;
	}

	public Func1<BOfflineNotify, Boolean> getOnOfflineNotify() {
		return onOfflineNotify;
	}

	public void setOnPrepare(Action1<SubscribeState> value) {
		onPrepare = value;
	}

	public Action2<SubscribeState, BServiceInfo> getOnRemoved() {
		return onRemove;
	}

	public void setOnRemoved(Action2<SubscribeState, BServiceInfo> value) {
		onRemove = value;
	}

	public Action2<SubscribeState, BServiceInfo> getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(Action2<SubscribeState, BServiceInfo> value) {
		onUpdate = value;
	}

	public Runnable getOnKeepAlive() {
		return onKeepAlive;
	}

	public void setOnKeepAlive(Runnable value) {
		onKeepAlive = value;
	}

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public final class SubscribeState {
		private final BSubscribeInfo subscribeInfo;
		private volatile BServiceInfos serviceInfos;
		private volatile BServiceInfos serviceInfosPending;

		/**
		 * 刚初始化时为false，任何修改ServiceInfos都会设置成true。 用来处理Subscribe返回的第一份数据和Commit可能乱序的问题。
		 * 目前的实现不会发生乱序。
		 */
		private boolean committed = false;
		// 服务准备好。
		public final ConcurrentHashMap<String, Object> localStates = new ConcurrentHashMap<>();

		@Override
		public String toString() {
			return getSubscribeType() + " " + serviceInfos;
		}

		public BSubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public int getSubscribeType() {
			return subscribeInfo.getSubscribeType();
		}

		public String getServiceName() {
			return subscribeInfo.getServiceName();
		}

		public BServiceInfos getServiceInfos() {
			return serviceInfos;
		}

		public BServiceInfos getServiceInfosPending() {
			return serviceInfosPending;
		}

		public SubscribeState(BSubscribeInfo info) {
			subscribeInfo = info;
			serviceInfos = new BServiceInfos(info.getServiceName());
		}

		// NOT UNDER LOCK
		@SuppressWarnings("UnusedReturnValue")
		private boolean trySendReadyServiceList() {
			var pending = serviceInfosPending;
			if (pending == null)
				return false;

			for (var p : pending.getServiceInfoListSortedByIdentity()) {
				if (!localStates.containsKey(p.getServiceIdentity()))
					return false;
			}
			var s = client.getSocket();
			if (s != null) {
				var r = new ReadyServiceList();
				r.Argument.serviceName = pending.getServiceName();
				r.Argument.serialId = pending.getSerialId();
				s.Send(r);
			}
			return true;
		}

		public void setServiceIdentityReadyState(String identity, Object state) {
			if (state == null)
				localStates.remove(identity);
			else
				localStates.put(identity, state);

			synchronized (this) {
				// 尝试发送Ready，如果有pending.
				trySendReadyServiceList();
			}
		}

		private void prepareAndTriggerOnChanged() {
			if (onChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void onRegister(BServiceInfo info) {
			serviceInfos.insert(info);
			if (onUpdate != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onUpdate.run(this, info);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			} else if (onChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void onUnRegister(BServiceInfo info) {
			var removed = serviceInfos.remove(info);
			if (removed == null)
				return;
			if (onRemove != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onRemove.run(this, removed);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			} else if (onChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void onUpdate(BServiceInfo info) {
			var exist = serviceInfos.findServiceInfo(info);
			if (exist == null)
				return;
			exist.setPassiveIp(info.getPassiveIp());
			exist.setPassivePort(info.getPassivePort());
			exist.setExtraInfo(info.getExtraInfo());

			if (onUpdate != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onUpdate.run(this, exist);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			} else if (onChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						onChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		public synchronized void onNotify(BServiceInfos infos) {
			switch (getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				serviceInfos = infos;
				committed = true;
				prepareAndTriggerOnChanged();
				break;

			case BSubscribeInfo.SubscribeTypeReadyCommit:
				if (serviceInfosPending == null || infos.getSerialId() > serviceInfosPending.getSerialId()) {
					serviceInfosPending = infos;
					if (onPrepare != null) {
						Task.getCriticalThreadPool().execute(() -> {
							try {
								onPrepare.run(this);
							} catch (Throwable e) {
								logger.error("", e);
							}
						});
					}
					trySendReadyServiceList();
				}
				break;
			}
		}

		public synchronized void onFirstCommit(BServiceInfos infos) {
			if (committed)
				return;
			if (getSubscribeType() == BSubscribeInfo.SubscribeTypeReadyCommit)
				return; // ReadyCommit 模式不会走到这里。OnNotify(infos);
			committed = true;
			serviceInfos = infos;
			serviceInfosPending = null;
			prepareAndTriggerOnChanged();
		}

		public synchronized void onCommit(BServiceListVersion version) {
			if (serviceInfosPending == null)
				return; // 并发过来的Commit，只需要处理一个。
			if (version.serialId != serviceInfosPending.getSerialId())
				logger.warn("OnCommit {} {} != {}", getServiceName(), version.serialId, serviceInfosPending.getSerialId());
			serviceInfos = serviceInfosPending;
			serviceInfosPending = null;
			committed = true;
			prepareAndTriggerOnChanged();
		}
	}

	public BServiceInfo registerService(String name, String identity) {
		return registerService(name, identity, null, 0, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip) {
		return registerService(name, identity, ip, 0, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip, int port) {
		return registerService(name, identity, ip, port, null);
	}

	public BServiceInfo registerService(String name, String identity, String ip, int port, Binary extraInfo) {
		return registerService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public BServiceInfo updateService(String name, String identity, String ip, int port, Binary extraInfo) {
		return updateService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public void waitConnectorReady() {
		// 实际上只有一个连接，这样就不用查找了。
		client.getConfig().forEachConnector(Connector::WaitReady);
	}

	private BServiceInfo updateService(BServiceInfo info) {
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

	private static void verify(String identity) {
		if (!identity.startsWith("@")) {
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(identity);
		}
	}

	private BServiceInfo registerService(BServiceInfo info) {
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
			} catch (Throwable e) {
				registers.remove(info, info); // rollback
				throw e;
			}
		}
		return regServInfo;
	}

	public void unRegisterService(String name, String identity) {
		unRegisterService(new BServiceInfo(name, identity));
	}

	private void unRegisterService(BServiceInfo info) {
		waitConnectorReady();

		var exist = registers.remove(info);
		if (exist != null) {
			try {
				new UnRegister(info).SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable e) {
				registers.putIfAbsent(exist, exist); // rollback
				throw e;
			}
		}
	}

	public SubscribeState subscribeService(String serviceName, int type) {
		return subscribeService(serviceName, type, null);
	}

	public SubscribeState subscribeService(String serviceName, int type, Object state) {
		if (type != BSubscribeInfo.SubscribeTypeSimple && type != BSubscribeInfo.SubscribeTypeReadyCommit)
			throw new UnsupportedOperationException("Unknown SubscribeType: " + type);

		var info = new BSubscribeInfo();
		info.setServiceName(serviceName);
		info.setSubscribeType(type);
		info.setLocalState(state);
		return subscribeService(info);
	}

	private SubscribeState subscribeService(BSubscribeInfo info) {
		waitConnectorReady();

		final var newAdd = new OutObject<Boolean>();
		newAdd.value = false;
		var subState = subscribeStates.computeIfAbsent(info.getServiceName(), __ -> {
			newAdd.value = true;
			return new SubscribeState(info);
		});

		if (newAdd.value) {
			var r = new Subscribe(info);
			r.SendAndWaitCheckResultCode(client.getSocket());
			logger.debug("SubscribeService {}", info);
		}
		return subState;
	}

	public void unSubscribeService(String serviceName) {
		waitConnectorReady();

		var state = subscribeStates.remove(serviceName);
		if (state != null) {
			try {
				var r = new UnSubscribe(state.subscribeInfo);
				r.SendAndWaitCheckResultCode(client.getSocket());
				logger.debug("UnSubscribeService {}", state.subscribeInfo);
			} catch (Throwable e) {
				subscribeStates.putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	public AutoKey getAutoKey(String name) {
		return autoKeys.computeIfAbsent(name, k -> new AutoKey(k, this));
	}

	public boolean setServerLoad(BServerLoad load) {
		return new SetServerLoad(load).Send(client.getSocket());
	}

	public void offlineRegister(BOfflineNotify argument) {
		waitConnectorReady();
		new OfflineRegister(argument).SendAndWaitCheckResultCode(client.getSocket());
	}

	public void onConnected() {
		for (var e : registers.keySet()) {
			try {
				new Register(e).SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable ex) {
				logger.debug("OnConnected.Register={}", e, ex);
			}
		}
		for (var e : subscribeStates.values()) {
			try {
				e.committed = false;
				var r = new Subscribe();
				r.Argument = e.subscribeInfo;
				r.SendAndWaitCheckResultCode(client.getSocket());
			} catch (Throwable ex) {
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
				} catch (Throwable e) {
					logger.error("", e);
				}
			});
		}
		return Procedure.Success;
	}

	private long processOfflineNotify(OfflineNotify r) {
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

	public Agent(Zeze.Application zeze) throws Throwable {
		this(zeze, null);
	}

	public Agent(Zeze.Application zeze, String netServiceName) throws Throwable {
		this.zeze = zeze;

		var config = zeze.getConfig();
		if (null == config) {
			throw new IllegalStateException("Config is null");
		}

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
	}

	public synchronized void stop() throws Throwable {
		if (client != null) {
			client.Stop();
			client = null;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}
}
