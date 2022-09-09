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

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	private final ConcurrentHashMap<String, SubscribeState> SubscribeStates = new ConcurrentHashMap<>();
	private AgentClient Client;
	private final Zeze.Application zeze;

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	private Action1<SubscribeState> OnChanged; // Simple Or ReadyCommit
	private Action2<SubscribeState, BServiceInfo> OnUpdate; // Simple
	private Action2<SubscribeState, BServiceInfo> OnRemove; // Simple
	private Action1<SubscribeState> OnPrepare; // ReadyCommit 的第一步回调。
	private Action1<BServerLoad> OnSetServerLoad;
	private Func1<BOfflineNotify, Boolean> OnOfflineNotify; // <serverId, notifyId, 是否处理成功且不需要其它notifier继续处理>

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	private Runnable OnKeepAlive;

	private final ConcurrentHashMap<BServiceInfo, BServiceInfo> Registers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AutoKey> AutoKeys = new ConcurrentHashMap<>();

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public static final String DefaultServiceName = "Zeze.Services.ServiceManager.Agent";

	public ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
		return SubscribeStates;
	}

	/*
	public Object getLocalState(String serviceName, String identity) {
		return SubscribeStates.get(serviceName).LocalStates.get(identity);
	}

	public Object getLocalState(ServiceInfo serviceInfo) {
		return getLocalState(serviceInfo.getServiceName(), serviceInfo.getServiceIdentity());
	}
	*/

	public final ConcurrentHashMap<String, BServerLoad> Loads = new ConcurrentHashMap<>();

	public AgentClient getClient() {
		return Client;
	}

	public Zeze.Application getZeze() {
		return zeze;
	}

	public Action1<SubscribeState> getOnChanged() {
		return OnChanged;
	}

	public void setOnChanged(Action1<SubscribeState> value) {
		OnChanged = value;
	}

	public void setOnSetServerLoad(Action1<BServerLoad> value) {
		OnSetServerLoad = value;
	}

	public void setOnOfflineNotify(Func1<BOfflineNotify, Boolean> value) {
		OnOfflineNotify = value;
	}

	public Func1<BOfflineNotify, Boolean> getOnOfflineNotify() {
		return OnOfflineNotify;
	}

	public void setOnPrepare(Action1<SubscribeState> value) {
		OnPrepare = value;
	}

	public Action2<SubscribeState, BServiceInfo> getOnRemoved() {
		return OnRemove;
	}

	public void setOnRemoved(Action2<SubscribeState, BServiceInfo> value) {
		OnRemove = value;
	}

	public Action2<SubscribeState, BServiceInfo> getOnUpdate() {
		return OnUpdate;
	}

	public void setOnUpdate(Action2<SubscribeState, BServiceInfo> value) {
		OnUpdate = value;
	}

	public Runnable getOnKeepAlive() {
		return OnKeepAlive;
	}

	public void setOnKeepAlive(Runnable value) {
		OnKeepAlive = value;
	}

	private ConcurrentHashMap<BServiceInfo, BServiceInfo> getRegisters() {
		return Registers;
	}

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public final class SubscribeState {
		private final BSubscribeInfo subscribeInfo;
		private volatile BServiceInfos ServiceInfos;
		private volatile BServiceInfos ServiceInfosPending;

		@Override
		public String toString() {
			return subscribeInfo.getSubscribeType() + " " + ServiceInfos;
		}

		/**
		 * 刚初始化时为false，任何修改ServiceInfos都会设置成true。 用来处理Subscribe返回的第一份数据和Commit可能乱序的问题。
		 * 目前的实现不会发生乱序。
		 */
		private boolean Committed = false;
		// 服务准备好。
		public final ConcurrentHashMap<String, Object> LocalStates = new ConcurrentHashMap<>();

		public BSubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public int getSubscribeType() {
			return getSubscribeInfo().getSubscribeType();
		}

		public String getServiceName() {
			return getSubscribeInfo().getServiceName();
		}

		public BServiceInfos getServiceInfos() {
			return ServiceInfos;
		}

		private void setServiceInfos(BServiceInfos value) {
			ServiceInfos = value;
		}

		public BServiceInfos getServiceInfosPending() {
			return ServiceInfosPending;
		}

		public boolean getCommitted() {
			return Committed;
		}

		public void setCommitted(boolean value) {
			Committed = value;
		}

		public Agent getAgent() {
			return Agent.this;
		}

		public SubscribeState(BSubscribeInfo info) {
			subscribeInfo = info;
			setServiceInfos(new BServiceInfos(info.getServiceName()));
		}

		// NOT UNDER LOCK
		@SuppressWarnings("UnusedReturnValue")
		private boolean TrySendReadyServiceList() {
			var pending = ServiceInfosPending;
			if (null == pending) {
				return false;
			}

			for (var p : pending.getServiceInfoListSortedByIdentity()) {
				if (!LocalStates.containsKey(p.getServiceIdentity())) {
					return false;
				}
			}
			var r = new ReadyServiceList();
			r.Argument.ServiceName = pending.getServiceName();
			r.Argument.SerialId = pending.getSerialId();
			if (getAgent().getClient().getSocket() != null) {
				getAgent().getClient().getSocket().Send(r);
			}
			return true;
		}

		public void SetServiceIdentityReadyState(String identity, Object state) {
			if (null == state) {
				LocalStates.remove(identity);
			} else {
				LocalStates.put(identity, state);
			}

			synchronized (this) {
				// 尝试发送Ready，如果有pending.
				TrySendReadyServiceList();
			}
		}

		private void PrepareAndTriggerOnChanged() {
			if (Agent.this.OnChanged != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						Agent.this.OnChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void OnUpdate(BServiceInfo info) {
			var exist = ServiceInfos.findServiceInfoByIdentity(info.getServiceIdentity());
			if (null == exist)
				return;

			exist.setPassiveIp(info.getPassiveIp());
			exist.setPassivePort(info.getPassivePort());
			exist.setExtraInfo(info.getExtraInfo());

			if (Agent.this.OnUpdate != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						Agent.this.OnUpdate.run(this, exist);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			} else if (null != Agent.this.OnChanged) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						Agent.this.OnChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void OnRegister(BServiceInfo info) {
			var info2 = ServiceInfos.Insert(info);
			if (Agent.this.OnUpdate != null) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						Agent.this.OnUpdate.run(this, info2);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			} else if (null != Agent.this.OnChanged) {
				Task.getCriticalThreadPool().execute(() -> {
					try {
						Agent.this.OnChanged.run(this);
					} catch (Throwable e) {
						logger.error("", e);
					}
				});
			}
		}

		synchronized void OnUnRegister(BServiceInfo info) {
			var info2 = ServiceInfos.Remove(info);
			if (null != info2) {
				if (Agent.this.OnRemove != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							Agent.this.OnRemove.run(this, info2);
						} catch (Throwable e) {
							logger.error("", e);
						}
					});
				} else if (Agent.this.OnChanged != null) {
					Task.getCriticalThreadPool().execute(() -> {
						try {
							Agent.this.OnChanged.run(this);
						} catch (Throwable e) {
							logger.error("", e);
						}
					});
				}
			}
		}

		public synchronized void OnNotify(BServiceInfos infos) {
			switch (getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				setServiceInfos(infos);
				setCommitted(true);
				PrepareAndTriggerOnChanged();
				break;

			case BSubscribeInfo.SubscribeTypeReadyCommit:
				if (null == getServiceInfosPending()
						|| infos.getSerialId() > getServiceInfosPending().getSerialId()) {
					ServiceInfosPending = infos;
					if (null != OnPrepare)
						Task.getCriticalThreadPool().execute(() -> {
							try {
								OnPrepare.run(this);
							} catch (Throwable e) {
								logger.error("", e);
							}
						});
					TrySendReadyServiceList();
				}
				break;
			}
		}

		public synchronized void OnCommit(CommitServiceList r) {
			if (ServiceInfosPending == null)
				return; // 并发过来的Commit，只需要处理一个。
			if (r.Argument.SerialId != ServiceInfosPending.getSerialId()) {
				logger.warn("OnCommit {} {} != {}", getServiceName(), r.Argument.SerialId, ServiceInfosPending.getSerialId());
			}
			setServiceInfos(ServiceInfosPending);
			ServiceInfosPending = null;
			setCommitted(true);
			PrepareAndTriggerOnChanged();
		}

		public synchronized void OnFirstCommit(BServiceInfos infos) {
			if (getCommitted()) {
				return;
			}
			if (subscribeInfo.getSubscribeType() == BSubscribeInfo.SubscribeTypeReadyCommit) {
				// ReadyCommit 模式不会走到这里。OnNotify(infos);
				return;
			}
			setCommitted(true);
			setServiceInfos(infos);
			ServiceInfosPending = null;
			PrepareAndTriggerOnChanged();
		}
	}

	public BServiceInfo RegisterService(String name, String identity) {
		return RegisterService(name, identity, null, 0, null);
	}

	public BServiceInfo RegisterService(String name, String identity, String ip) {
		return RegisterService(name, identity, ip, 0, null);
	}

	public BServiceInfo RegisterService(String name, String identity, String ip, int port) {
		return RegisterService(name, identity, ip, port, null);
	}

	public BServiceInfo RegisterService(String name, String identity, String ip, int port, Binary extraInfo) {
		return RegisterService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public BServiceInfo UpdateService(String name, String identity, String ip, int port, Binary extraInfo) {
		return UpdateService(new BServiceInfo(name, identity, ip, port, extraInfo));
	}

	public void WaitConnectorReady() {
		// 实际上只有一个连接，这样就不用查找了。
		Client.getConfig().forEachConnector(Connector::WaitReady);
	}

	private BServiceInfo UpdateService(BServiceInfo info) {
		WaitConnectorReady();
		var reg = Registers.get(info);
		if (null == reg)
			return null;

		var r = new Update();
		r.Argument = info;
		r.SendAndWaitCheckResultCode(Client.getSocket());

		reg.setPassiveIp(info.getPassiveIp());
		reg.setPassivePort(info.getPassivePort());
		reg.setExtraInfo(info.getExtraInfo());

		return reg;
	}

	private static void Verify(String identity) {
		if (!identity.startsWith("@")) {
			//noinspection ResultOfMethodCallIgnored
			Integer.parseInt(identity);
		}
	}

	private BServiceInfo RegisterService(BServiceInfo info) {
		Verify(info.getServiceIdentity());
		WaitConnectorReady();

		var regNew = new OutObject<Boolean>();
		regNew.Value = false;
		var regServInfo = getRegisters().computeIfAbsent(info, (key) -> {
			regNew.Value = true;
			return key;
		});

		if (regNew.Value) {
			try {
				new Register(info).SendAndWaitCheckResultCode(Client.getSocket());
				logger.debug("RegisterService {}", info);
			} catch (Throwable e) {
				getRegisters().remove(info, info); // rollback
				throw e;
			}
		}
		return regServInfo;
	}

	public void UnRegisterService(String name, String identity) {
		UnRegisterService(new BServiceInfo(name, identity));
	}

	private void UnRegisterService(BServiceInfo info) {
		WaitConnectorReady();

		var exist = getRegisters().remove(info);
		if (null != exist) {
			try {
				var r = new UnRegister();
				r.Argument = info;
				r.SendAndWaitCheckResultCode(Client.getSocket());
			} catch (Throwable e) {
				getRegisters().putIfAbsent(exist, exist); // rollback
				throw e;
			}
		}
	}

	public SubscribeState SubscribeService(String serviceName, int type) {
		return SubscribeService(serviceName, type, null);
	}

	public SubscribeState SubscribeService(String serviceName, int type, Object state) {
		if (type != BSubscribeInfo.SubscribeTypeSimple && type != BSubscribeInfo.SubscribeTypeReadyCommit) {
			throw new UnsupportedOperationException("Unknown SubscribeType: " + type);
		}

		BSubscribeInfo tempVar = new BSubscribeInfo();
		tempVar.setServiceName(serviceName);
		tempVar.setSubscribeType(type);
		tempVar.setLocalState(state);
		return SubscribeService(tempVar);
	}

	private SubscribeState SubscribeService(BSubscribeInfo info) {
		WaitConnectorReady();

		final var newAdd = new OutObject<Boolean>();
		newAdd.Value = false;
		var subState = SubscribeStates.computeIfAbsent(info.getServiceName(), (key) -> {
			newAdd.Value = true;
			return new SubscribeState(info);
		});

		if (newAdd.Value) {
			var r = new Subscribe();
			r.Argument = info;
			r.SendAndWaitCheckResultCode(Client.getSocket());
			logger.debug("SubscribeService {}", info);
		}
		return subState;
	}

	public boolean SetServerLoad(BServerLoad load) {
		return new SetServerLoad(load).Send(Client.getSocket());
	}

	public void OfflineRegister(BOfflineNotify argument) {
		WaitConnectorReady();
		new OfflineRegister(argument).SendAndWaitCheckResultCode(Client.getSocket());
	}

	private long ProcessSubscribeFirstCommit(SubscribeFirstCommit r) {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (state != null) {
			state.OnFirstCommit(r.Argument);
		}
		return Procedure.Success;
	}

	public void UnSubscribeService(String serviceName) {
		WaitConnectorReady();

		var state = SubscribeStates.remove(serviceName);
		if (state != null) {
			try {
				var r = new UnSubscribe();
				r.Argument = state.subscribeInfo;
				r.SendAndWaitCheckResultCode(Client.getSocket());
			} catch (Throwable e) {
				SubscribeStates.putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	private long ProcessUpdate(Update r) {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;

		state.OnUpdate(r.Argument);
		r.SendResult();
		return 0;
	}

	private long ProcessRegister(Register r) {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;

		state.OnRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	private long ProcessUnRegister(UnRegister r) {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServiceNotSubscribe;

		state.OnUnRegister(r.Argument);
		r.SendResult();
		return 0;
	}

	private long ProcessNotifyServiceList(NotifyServiceList r) {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (state != null) {
			state.OnNotify(r.Argument);
		} else {
			Agent.logger.warn("NotifyServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private long ProcessCommitServiceList(CommitServiceList r) {
		var state = SubscribeStates.get(r.Argument.ServiceName);
		if (state != null) {
			state.OnCommit(r);
		} else {
			Agent.logger.warn("CommitServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private long ProcessKeepAlive(KeepAlive r) {
		if (OnKeepAlive != null) {
			Task.getCriticalThreadPool().execute(OnKeepAlive);
		}
		r.SendResultCode(KeepAlive.Success);
		return Procedure.Success;
	}

	private long ProcessSetServerLoad(SetServerLoad setServerLoad) {
		Loads.put(setServerLoad.Argument.getName(), setServerLoad.Argument);
		if (null != OnSetServerLoad)
			Task.getCriticalThreadPool().execute(() -> {
				try {
					OnSetServerLoad.run(setServerLoad.Argument);
				} catch (Throwable e) {
					logger.error("", e);
				}
			});
		return Procedure.Success;
	}

	private long ProcessOfflineNotify(OfflineNotify r) {
		if (null == OnOfflineNotify) {
			r.trySendResultCode(1);
			return 0;
		}
		try {
			if (OnOfflineNotify.call(r.Argument)) {
				r.SendResult();
				return 0;
			}
			r.trySendResultCode(2);
		} catch (Throwable ignored) {
			r.trySendResultCode(3);
		}
		return 0;
	}

	public AutoKey GetAutoKey(String name) {
		return AutoKeys.computeIfAbsent(name, (k) -> new AutoKey(k, this));
	}

	public synchronized void Stop() throws Throwable {
		if (Client != null) {
			Client.Stop();
			Client = null;
		}
	}

	public void OnConnected() {
		for (var e : Registers.keySet()) {
			try {
				new Register(e).SendAndWaitCheckResultCode(Client.getSocket());
			} catch (Throwable ex) {
				logger.debug("OnConnected.Register={}", e, ex);
			}
		}
		for (var e : SubscribeStates.values()) {
			try {
				e.Committed = false;
				var r = new Subscribe();
				r.Argument = e.subscribeInfo;
				r.SendAndWaitCheckResultCode(Client.getSocket());
			} catch (Throwable ex) {
				logger.debug("OnConnected.Subscribe={}", e.subscribeInfo, ex);
			}
		}
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

		Client = (null == netServiceName || netServiceName.isEmpty())
				? new AgentClient(this, config)
				: new AgentClient(this, config, netServiceName);

		Client.AddFactoryHandle(Register.TypeId_, new ProtocolFactoryHandle<>(
				Register::new, this::ProcessRegister, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(UnRegister.TypeId_, new ProtocolFactoryHandle<>(
				UnRegister::new, this::ProcessUnRegister, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(Update.TypeId_, new ProtocolFactoryHandle<>(
				Update::new, this::ProcessUpdate, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(Subscribe.TypeId_, new ProtocolFactoryHandle<>(
				Subscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(UnSubscribe.TypeId_, new ProtocolFactoryHandle<>(
				UnSubscribe::new, null, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(NotifyServiceList.TypeId_, new ProtocolFactoryHandle<>(
				NotifyServiceList::new, this::ProcessNotifyServiceList, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(CommitServiceList.TypeId_, new ProtocolFactoryHandle<>(
				CommitServiceList::new, this::ProcessCommitServiceList, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
				KeepAlive::new, this::ProcessKeepAlive, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(SubscribeFirstCommit.TypeId_, new ProtocolFactoryHandle<>(
				SubscribeFirstCommit::new, this::ProcessSubscribeFirstCommit, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(AllocateId.TypeId_, new ProtocolFactoryHandle<>(
				AllocateId::new, null, TransactionLevel.None, DispatchMode.Direct));
		Client.AddFactoryHandle(SetServerLoad.TypeId_, new ProtocolFactoryHandle<>(
				SetServerLoad::new, this::ProcessSetServerLoad, TransactionLevel.None, DispatchMode.Direct));

		Client.AddFactoryHandle(OfflineNotify.TypeId_, new ProtocolFactoryHandle<>(
				OfflineNotify::new, this::ProcessOfflineNotify, TransactionLevel.None, DispatchMode.Critical));
		Client.AddFactoryHandle(OfflineRegister.TypeId_, new ProtocolFactoryHandle<>(
				OfflineRegister::new, null, TransactionLevel.None, DispatchMode.Normal));
	}

	@Override
	public void close() throws IOException {
		try {
			Stop();
		} catch (Throwable e) {
			throw new IOException(e);
		}
	}
}
