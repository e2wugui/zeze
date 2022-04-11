package Zeze.Services.ServiceManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.Service.ProtocolFactoryHandle;
import Zeze.Transaction.Procedure;
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
	private Zeze.Util.Action1<SubscribeState> OnChanged;
	private Zeze.Util.Action2<SubscribeState, ServiceInfo> OnUpdate;
	private Zeze.Util.Action2<SubscribeState, ServiceInfo> OnRemove;
	private Zeze.Util.Action1<SubscribeState> OnPrepare; // ReadyCommit 的第一步回调。
	private Zeze.Util.Action1<ServerLoad> OnSetServerLoad;

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	private Runnable OnKeepAlive;

	private final ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AutoKey> AutoKeys = new ConcurrentHashMap<>();

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */
	public final static String DefaultServiceName = "Zeze.Services.ServiceManager.Agent";

	public ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
		return SubscribeStates;
	}

	public ConcurrentHashMap<String, ServerLoad> Loads = new ConcurrentHashMap<>();

	public AgentClient getClient() {
		return Client;
	}

	public Zeze.Application getZeze() {
		return zeze;
	}

	public Zeze.Util.Action1<SubscribeState> getOnChanged() {
		return OnChanged;
	}

	public void setOnChanged(Zeze.Util.Action1<SubscribeState> value) {
		OnChanged = value;
	}
	public void setOnSetServerLoad(Zeze.Util.Action1<ServerLoad> value) { OnSetServerLoad = value; }

	public void setOnPrepare(Zeze.Util.Action1<SubscribeState> value) { OnPrepare = value; }

	public Zeze.Util.Action2<SubscribeState, ServiceInfo> getOnRemoved() {
		return OnRemove;
	}

	public void setOnRemoved(Zeze.Util.Action2<SubscribeState, ServiceInfo> value) {
		OnRemove = value;
	}

	public Zeze.Util.Action2<SubscribeState, ServiceInfo> getOnUpdate() {
		return OnUpdate;
	}

	public void setOnUpdate(Zeze.Util.Action2<SubscribeState, ServiceInfo> value) {
		OnUpdate = value;
	}

	public Runnable getOnKeepAlive() {
		return OnKeepAlive;
	}

	public void setOnKeepAlive(Runnable value) {
		OnKeepAlive = value;
	}

	private ConcurrentHashMap<ServiceInfo, ServiceInfo> getRegisters() {
		return Registers;
	}

	// 【警告】
	// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
	// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
	// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
	public final class SubscribeState {
		public Agent getAgent() {
			return Agent.this;
		}

		private final SubscribeInfo subscribeInfo;

		public SubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public int getSubscribeType() {
			return getSubscribeInfo().getSubscribeType();
		}

		public String getServiceName() {
			return getSubscribeInfo().getServiceName();
		}

		private volatile ServiceInfos ServiceInfos;

		public ServiceInfos getServiceInfos() {
			return ServiceInfos;
		}

		private void setServiceInfos(ServiceInfos value) {
			ServiceInfos = value;
		}

		private volatile ServiceInfos ServiceInfosPending;

		public ServiceInfos getServiceInfosPending() {
			return ServiceInfosPending;
		}

		/**
		 * 刚初始化时为false，任何修改ServiceInfos都会设置成true。 用来处理Subscribe返回的第一份数据和Commit可能乱序的问题。
		 * 目前的实现不会发生乱序。
		 */
		private boolean Committed = false;

		public boolean getCommitted() {
			return Committed;
		}

		public void setCommitted(boolean value) {
			Committed = value;
		}

		// 服务准备好。
		private final ConcurrentHashMap<String, Object> ServiceIdentityReadyStates = new ConcurrentHashMap<>();

		public SubscribeState(SubscribeInfo info) {
			subscribeInfo = info;
			setServiceInfos(new ServiceInfos(info.getServiceName()));
		}

		// NOT UNDER LOCK
		@SuppressWarnings("UnusedReturnValue")
		private boolean TrySendReadyServiceList() {
			var pending = ServiceInfosPending;
			if (null == pending) {
				return false;
			}

			System.out.println("ServerId=" + Agent.this.getZeze().getConfig().getServerId() + "Pending=" + pending + " Readys=" + ServiceIdentityReadyStates.keySet());

			for (var p : pending.getServiceInfoListSortedByIdentity()) {
				if (!ServiceIdentityReadyStates.containsKey(p.getServiceIdentity())) {
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
				ServiceIdentityReadyStates.remove(identity);
			} else {
				ServiceIdentityReadyStates.put(identity, state);
			}

			synchronized (this) {
				// 把 state 复制到当前版本的服务列表中。允许列表不变，服务状态改变。
				if (null != ServiceInfos) {
					ServiceInfo info = ServiceInfos.get(identity);
					if (null != info)
						info.setLocalState(state);
				}
			}
			// 尝试发送Ready，如果有pending.
			TrySendReadyServiceList();
		}

		private void PrepareAndTriggerOnChanged() throws Throwable {
			for (var info : getServiceInfos().getServiceInfoListSortedByIdentity()) {
				var state = ServiceIdentityReadyStates.get(info.getServiceIdentity());
				if (null != state) // 需要确认里面会不会存null。
					info.setLocalState(state);
			}
			if (Agent.this.OnChanged != null) {
				Zeze.Util.Task.Run(() -> Agent.this.OnChanged.run(this), "ServiceManager.Agent.OnChanged");
			}
		}

		synchronized void OnUpdate(ServiceInfo info) throws Throwable {
			var exist = ServiceInfos.findServiceInfoByIdentity(info.getServiceIdentity());
			if (null == exist)
				return;

			exist.setPassiveIp(info.getPassiveIp());
			exist.setPassivePort(info.getPassivePort());
			exist.setExtraInfo(info.getExtraInfo());

			if (Agent.this.OnUpdate != null)
				Zeze.Util.Task.Run(() -> Agent.this.OnUpdate.run(this, exist), "ServiceManager.Agent.OnUpdate");
			else if (null != Agent.this.OnChanged)
				Zeze.Util.Task.Run(() -> Agent.this.OnChanged.run(this), "ServiceManager.Agent.OnUpdate.OnChanged");
		}

		synchronized void OnRegister(ServiceInfo info) throws Throwable {
			//noinspection ConstantConditions
			var info2 = ServiceInfos.Insert(info);
			if (Agent.this.OnUpdate != null)
				Zeze.Util.Task.Run(() -> Agent.this.OnUpdate.run(this, info2), "ServiceManager.Agent.OnUpdate");
			else if (null != Agent.this.OnChanged)
				Zeze.Util.Task.Run(() -> Agent.this.OnChanged.run(this), "ServiceManager.Agent.OnUpdate.OnChanged");
		}

		synchronized void OnUnRegister(ServiceInfo info) throws Throwable {
			var info2 = ServiceInfos.Remove(info);
			if (null != info2) {
				if (Agent.this.OnRemove != null)
					Zeze.Util.Task.Run(() -> Agent.this.OnRemove.run(this, info2), "ServiceManager.Agent.OnRemove");
				else if (Agent.this.OnChanged != null)
					Zeze.Util.Task.Run(() -> Agent.this.OnChanged.run(this), "ServiceManager.Agent.OnRemove.OnChanged");
			}
		}

		public synchronized void OnNotify(ServiceInfos infos) throws Throwable {
			switch (getSubscribeType()) {
			case SubscribeInfo.SubscribeTypeSimple:
				setServiceInfos(infos);
				setCommitted(true);
				PrepareAndTriggerOnChanged();
				break;

			case SubscribeInfo.SubscribeTypeReadyCommit:
				if (null == getServiceInfosPending()
						|| infos.getSerialId() > getServiceInfosPending().getSerialId()) {
					ServiceInfosPending = infos;
					if (null != OnPrepare)
						Zeze.Util.Task.Run(() -> OnPrepare.run(this), "ServiceManager.Agent.OnPrepare");
					TrySendReadyServiceList();
				}
				break;
			}
		}

		private boolean SequenceEqual(ArrayList<ServiceInfo> l1, ArrayList<ServiceInfo> l2) {
			if (l1.size() != l2.size())
				return false;

			for (int i = 0; i < l1.size(); ++i) {
				if (!l1.get(i).equals(l2.get(i)))
					return false;
			}
			return true;
		}

		public synchronized void OnCommit(CommitServiceList r) throws Throwable {
			if (r.Argument.SerialId != ServiceInfosPending.getSerialId())
			{
				logger.warn("OnCommit " + getServiceName() + " " + r.Argument.SerialId + " != " + ServiceInfosPending.getSerialId());
			}
			setServiceInfos(ServiceInfosPending);
			ServiceInfosPending = null;
			setCommitted(true);
			PrepareAndTriggerOnChanged();
		}

		public synchronized void OnFirstCommit(ServiceInfos infos) throws Throwable {
			if (getCommitted()) {
				return;
			}
			setCommitted(true);
			setServiceInfos(infos);
			ServiceInfosPending = null;
			PrepareAndTriggerOnChanged();
		}
	}

	public ServiceInfo RegisterService(String name, String identity, String ip, int port) throws Throwable {
		return RegisterService(name, identity, ip, port, null);
	}

	public ServiceInfo RegisterService(String name, String identity, String ip) throws Throwable {
		return RegisterService(name, identity, ip, 0, null);
	}

	public ServiceInfo RegisterService(String name, String identity) throws Throwable {
		return RegisterService(name, identity, null, 0, null);
	}

	public ServiceInfo RegisterService(String name, String identity, String ip, int port, Zeze.Net.Binary extraInfo) throws Throwable {
		return RegisterService(new ServiceInfo(name, identity, ip, port, extraInfo));
	}

	public ServiceInfo UpdateService(String name, String identity, String ip, int port, Zeze.Net.Binary extraInfo) throws Throwable {
		return UpdateService(new ServiceInfo(name, identity, ip, port, extraInfo));
	}

	public void WaitConnectorReady() throws Throwable {
		// 实际上只有一个连接，这样就不用查找了。
		getClient().getConfig().ForEachConnector(Connector::WaitReady);
	}

	private ServiceInfo UpdateService(ServiceInfo info) throws Throwable {
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

	private ServiceInfo RegisterService(ServiceInfo info) throws Throwable {
		WaitConnectorReady();

		var regNew = new Zeze.Util.OutObject<Boolean>();
		regNew.Value = false;
		var regServInfo = getRegisters().computeIfAbsent(info, (key) -> {
			regNew.Value = true;
			return key;
		});

		if (regNew.Value) {
			try {
				var r = new Register();
				r.Argument = info;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (Throwable e) {
				getRegisters().remove(info, info); // rollback
				throw e;
			}
		}
		return regServInfo;
	}

	public void UnRegisterService(String name, String identity) throws Throwable {
		UnRegisterService(new ServiceInfo(name, identity));
	}

	private void UnRegisterService(ServiceInfo info) throws Throwable {
		WaitConnectorReady();

		var exist = getRegisters().remove(info);
		if (null != exist) {
			try {
				var r = new UnRegister();
				r.Argument = info;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (Throwable e) {
				getRegisters().putIfAbsent(exist, exist); // rollback
				throw e;
			}
		}
	}

	public SubscribeState SubscribeService(String serviceName, int type) throws Throwable {
		return SubscribeService(serviceName, type, null);
	}

	public SubscribeState SubscribeService(String serviceName, int type, Object state) throws Throwable {
		if (type != SubscribeInfo.SubscribeTypeSimple && type != SubscribeInfo.SubscribeTypeReadyCommit) {
			throw new UnsupportedOperationException("Unknown SubscribeType: " + type);
		}

		SubscribeInfo tempVar = new SubscribeInfo();
		tempVar.setServiceName(serviceName);
		tempVar.setSubscribeType(type);
		tempVar.setLocalState(state);
		return SubscribeService(tempVar);
	}

	private SubscribeState SubscribeService(SubscribeInfo info) throws Throwable {
		WaitConnectorReady();

		final var newAdd = new Zeze.Util.OutObject<Boolean>();
		newAdd.Value = false;
		var subState = getSubscribeStates().computeIfAbsent(info.getServiceName(), (key) -> {
			newAdd.Value = true;
			return new SubscribeState(info);
		});

		if (newAdd.Value) {
			var r = new Subscribe();
			r.Argument = info;
			r.SendAndWaitCheckResultCode(getClient().getSocket());
		}
		return subState;
	}

	public boolean SetServerLoad(ServerLoad load) {
		try {
			WaitConnectorReady();
		} catch (Throwable e) {
			return false;
		}
		var p = new SetServerLoad();
		p.Argument = load;
		return p.Send(getClient().getSocket());
	}


	private long ProcessSubscribeFirstCommit(SubscribeFirstCommit r) throws Throwable {
		var state = getSubscribeStates().get(r.Argument.getServiceName());
		if (null != state) {
			state.OnFirstCommit(r.Argument);
		}
		return Procedure.Success;
	}

	public void UnSubscribeService(String serviceName) throws Throwable {
		WaitConnectorReady();

		var state = getSubscribeStates().remove(serviceName);
		if (null != state) {
			try {
				var r = new UnSubscribe();
				r.Argument = state.subscribeInfo;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (Throwable e) {
				getSubscribeStates().putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	private long ProcessUpdate(Update r) throws Throwable {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (null == state)
			return Update.ServiceNotSubscribe;

		state.OnUpdate(r.Argument);

		return 0;
	}

	private long ProcessRegister(Register r) throws Throwable {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (null == state)
			return Update.ServiceNotSubscribe;

		state.OnRegister(r.Argument);

		return 0;
	}

	private long ProcessUnRegister(UnRegister r) throws Throwable {
		var state = SubscribeStates.get(r.Argument.getServiceName());
		if (null == state)
			return Update.ServiceNotSubscribe;

		state.OnUnRegister(r.Argument);

		return 0;
	}

	private long ProcessNotifyServiceList(NotifyServiceList r) throws Throwable {
		var state = getSubscribeStates().get(r.Argument.getServiceName());
		if (null != state) {
			state.OnNotify(r.Argument);
		} else {
			Agent.logger.warn("NotifyServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private long ProcessCommitServiceList(CommitServiceList r) throws Throwable {
		var state = getSubscribeStates().get(r.Argument.ServiceName);
		if (null != state) {
			state.OnCommit(r);
		} else {
			Agent.logger.warn("CommitServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private long ProcessKeepAlive(KeepAlive r) {
		if (OnKeepAlive != null) {
			Task.Run(OnKeepAlive::run, "OnKeepAlive");
		}
		r.SendResultCode(KeepAlive.Success);
		return Procedure.Success;
	}

	private long ProcessSetServerLoad(SetServerLoad setServerLoad) throws Throwable {
		Loads.put(setServerLoad.Argument.getName(), setServerLoad.Argument);
		if (null != OnSetServerLoad)
			Task.Run(() -> OnSetServerLoad.run(setServerLoad.Argument), "OnSetServerLoad");
		return Procedure.Success;
	}

	private ConcurrentHashMap<String, AutoKey> getAutoKeys() {
		return AutoKeys;
	}

	public AutoKey GetAutoKey(String name) {
		return getAutoKeys().computeIfAbsent(name, (k) -> new AutoKey(k, this));
	}

	public synchronized void Stop() throws Throwable {
		if (null == Client) {
			return;
		}
		Client.Stop();
		Client = null;
	}

	public void OnConnected() {
		for (var e : Registers.entrySet()) {
			try {
				var r = new Register();
				r.Argument = e.getValue();
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (Throwable ex) {
				logger.debug("OnConnected.Register={}", e.getValue(), ex);
			}
		}
		for (var e : getSubscribeStates().entrySet()) {
			try {
				e.getValue().Committed = false;
				var r = new Subscribe();
				r.Argument = e.getValue().subscribeInfo;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (Throwable ex) {
				logger.debug("OnConnected.Subscribe={}", e.getValue().subscribeInfo, ex);
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

		Client.AddFactoryHandle(Register.TypeId_,
				new ProtocolFactoryHandle<>(Register::new, this::ProcessRegister));

		Client.AddFactoryHandle(UnRegister.TypeId_,
				new ProtocolFactoryHandle<>(UnRegister::new, this::ProcessUnRegister));

		Client.AddFactoryHandle(Update.TypeId_,
				new Service.ProtocolFactoryHandle<>(Update::new, this::ProcessUpdate));

		Client.AddFactoryHandle(Subscribe.TypeId_,
				new ProtocolFactoryHandle<>(Subscribe::new));

		Client.AddFactoryHandle(UnSubscribe.TypeId_,
				new ProtocolFactoryHandle<>(UnSubscribe::new));

		Client.AddFactoryHandle(NotifyServiceList.TypeId_,
				new ProtocolFactoryHandle<>(NotifyServiceList::new, this::ProcessNotifyServiceList));

		Client.AddFactoryHandle(CommitServiceList.TypeId_,
				new ProtocolFactoryHandle<>(CommitServiceList::new, this::ProcessCommitServiceList));

		Client.AddFactoryHandle(KeepAlive.TypeId_,
				new ProtocolFactoryHandle<>(KeepAlive::new, this::ProcessKeepAlive));

		Client.AddFactoryHandle(SubscribeFirstCommit.TypeId_,
				new Service.ProtocolFactoryHandle<>(SubscribeFirstCommit::new, this::ProcessSubscribeFirstCommit));

		Client.AddFactoryHandle(AllocateId.TypeId_,
				new ProtocolFactoryHandle<>(AllocateId::new));

		Client.AddFactoryHandle(SetServerLoad.TypeId_,
				new ProtocolFactoryHandle<>(SetServerLoad::new, this::ProcessSetServerLoad));
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
