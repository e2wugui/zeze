package Zeze.Services.ServiceManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Transaction.Procedure;
import Zeze.Net.*;
import Zeze.Net.Service.ProtocolFactoryHandle;

public final class Agent implements Closeable {
	static final Logger logger = LogManager.getLogger(Agent.class);

	// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
	// ServiceName ->
	private ConcurrentHashMap<String, SubscribeState> SubscribeStates = new ConcurrentHashMap<>();

	public ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
		return SubscribeStates;
	}

	private AgentClient Client;

	public AgentClient getClient() {
		return Client;
	}

	/**
	 * 订阅服务状态发生变化时回调。 如果需要处理这个事件，请在订阅前设置回调。
	 */
	private Zeze.Util.Action1<SubscribeState> OnChanged;

	public Zeze.Util.Action1<SubscribeState> getOnChanged() {
		return OnChanged;
	}

	public void setOnChanged(Zeze.Util.Action1<SubscribeState> value) {
		OnChanged = value;
	}

	// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
	// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
	private java.lang.Runnable OnKeepAlive;

	public java.lang.Runnable getOnKeepAlive() {
		return OnKeepAlive;
	}

	public void setOnKeepAlive(java.lang.Runnable value) {
		OnKeepAlive = value;
	}

	private ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new ConcurrentHashMap<>();

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

		private SubscribeInfo subscribeInfo;

		public SubscribeInfo getSubscribeInfo() {
			return subscribeInfo;
		}

		public int getSubscribeType() {
			return getSubscribeInfo().getSubscribeType();
		}

		public String getServiceName() {
			return getSubscribeInfo().getServiceName();
		}

		private ServiceInfos ServiceInfos;

		public ServiceInfos getServiceInfos() {
			return ServiceInfos;
		}

		private void setServiceInfos(ServiceInfos value) {
			ServiceInfos = value;
		}

		private ServiceInfos ServiceInfosPending;

		public ServiceInfos getServiceInfosPending() {
			return ServiceInfosPending;
		}

		private void setServiceInfosPending(ServiceInfos value) {
			ServiceInfosPending = value;
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
		private java.util.concurrent.ConcurrentHashMap<String, Object> ServiceIdentityReadyStates = new java.util.concurrent.ConcurrentHashMap<String, Object>();

		public java.util.concurrent.ConcurrentHashMap<String, Object> getServiceIdentityReadyStates() {
			return ServiceIdentityReadyStates;
		}

		public SubscribeState(SubscribeInfo info) {
			subscribeInfo = info;
			setServiceInfos(new ServiceInfos(info.getServiceName()));
		}

		// NOT UNDER LOCK
		private boolean TrySendReadyServiceList() {
			if (null == getServiceInfosPending()) {
				return false;
			}

			for (var pending : getServiceInfosPending().getServiceInfoListSortedByIdentity()) {
				if (!getServiceIdentityReadyStates().containsKey(pending.getServiceIdentity())) {
					return false;
				}
			}
			var r = new ReadyServiceList();
			r.Argument = getServiceInfosPending();
			if (getAgent().getClient().getSocket() != null) {
				getAgent().getClient().getSocket().Send(r);
			}
			return true;
		}

		public void SetServiceIdentityReadyState(String identity, Object state) {
			if (null == state) {
				ServiceIdentityReadyStates.remove(identity);
			} else {
				getServiceIdentityReadyStates().put(identity, state);
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

		private void PrepareAndTriggerOnchanged() {
			for (var info : getServiceInfos().getServiceInfoListSortedByIdentity()) {
				var state = getServiceIdentityReadyStates().get(info.getServiceIdentity());
				if (null != state) // 需要确认里面会不会存null。
					info.setLocalState(state);
			}
			if (Agent.this.OnChanged != null) {
				Agent.this.OnChanged.run(this);
			}
		}

		public void OnNotify(ServiceInfos infos) {
			synchronized (this) {
				switch (getSubscribeType()) {
				case SubscribeInfo.SubscribeTypeSimple:
					setServiceInfos(infos);
					setCommitted(true);
					PrepareAndTriggerOnchanged();
					break;

				case SubscribeInfo.SubscribeTypeReadyCommit:
					if (null == getServiceInfosPending()
							|| infos.getSerialId() > getServiceInfosPending().getSerialId()) {
						setServiceInfosPending(infos);
						TrySendReadyServiceList();
					}
					break;
				}
			}
		}

		private boolean SequenceEqual(ArrayList<ServiceInfo> l1, ArrayList<ServiceInfo> l2) {
			if (l1.size() != l2.size())
				return false;
			
			for (int i = 0; i < l1.size(); ++i) {
				if (false == l1.get(i).equals(l2.get(i)))
					return false;
			}
			return true;
		}

		public void OnCommit(ServiceInfos infos) {
			synchronized (this) {
				// ServiceInfosPending 和 Commit.infos 应该一样，否则肯定哪里出错了。
				// 这里总是使用最新的 Commit.infos，检查记录日志。
				if (!SequenceEqual(infos.getServiceInfoListSortedByIdentity(),
						getServiceInfosPending().getServiceInfoListSortedByIdentity())) {
					Agent.logger.warn("OnCommit: ServiceInfosPending Miss Match.");
				}
				setServiceInfos(infos);
				setServiceInfosPending(null);
				setCommitted(true);
				PrepareAndTriggerOnchanged();
			}
		}

		public void OnFirstCommit(ServiceInfos infos) {
			synchronized (this) {
				if (getCommitted()) {
					return;
				}
				setCommitted(true);
				setServiceInfos(infos);
				setServiceInfosPending(null);
				PrepareAndTriggerOnchanged();
			}
		}
	}

	public ServiceInfo RegisterService(String name, String identity, String ip, int port) {
		return RegisterService(name, identity, ip, port, null);
	}

	public ServiceInfo RegisterService(String name, String identity, String ip) {
		return RegisterService(name, identity, ip, 0, null);
	}

	public ServiceInfo RegisterService(String name, String identity) {
		return RegisterService(name, identity, null, 0, null);
	}

	public ServiceInfo RegisterService(String name, String identity, String ip, int port, Binary extrainfo) {
		return RegisterService(new ServiceInfo(name, identity, ip, port, extrainfo));
	}

	public void WaitConnectorReady() {
		// 实际上只有一个连接，这样就不用查找了。
		getClient().getConfig().ForEachConnector((c) -> c.WaitReady());
	}

	private ServiceInfo RegisterService(ServiceInfo info) {
		WaitConnectorReady();

		var regNew = new Zeze.Util.OutObject<Boolean>();
		regNew.Value = false;
		var regServInfo = getRegisters().computeIfAbsent(info, (key) -> {
			regNew.Value = true;
			return info;
		});

		if (regNew.Value) {
			try {
				var r = new Register();
				r.Argument = info;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (RuntimeException e) {
				getRegisters().remove(info, info); // rollback
				throw e;
			}
		}
		return regServInfo;
	}

	public void UnRegisterService(String name, String identity) {
		UnRegisterService(new ServiceInfo(name, identity));
	}

	private void UnRegisterService(ServiceInfo info) {
		WaitConnectorReady();

		var exist = getRegisters().remove(info);
		if (null != exist) {
			try {
				var r = new UnRegister();
				r.Argument = info;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (RuntimeException e) {
				getRegisters().putIfAbsent(exist, exist); // rollback
				throw e;
			}
		}
	}

	public SubscribeState SubscribeService(String serviceName, int type) {
		return SubscribeService(serviceName, type, null);
	}

	public SubscribeState SubscribeService(String serviceName, int type, Object state) {
		if (type != SubscribeInfo.SubscribeTypeSimple && type != SubscribeInfo.SubscribeTypeReadyCommit) {
			throw new RuntimeException("Unkown SubscribeType");
		}

		SubscribeInfo tempVar = new SubscribeInfo();
		tempVar.setServiceName(serviceName);
		tempVar.setSubscribeType(type);
		tempVar.setLocalState(state);
		return SubscribeService(tempVar);
	}

	private SubscribeState SubscribeService(SubscribeInfo info) {
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

	private int ProcessSubscribeFirstCommit(Protocol p) {
		var r = p instanceof SubscribeFirstCommit ? (SubscribeFirstCommit) p : null;
		var state = getSubscribeStates().get(r.Argument.getServiceName());
		if (null != state) {
			state.OnFirstCommit(r.Argument);
		}
		return Procedure.Success;
	}

	public void UnSubscribeService(String serviceName) {
		WaitConnectorReady();

		var state = getSubscribeStates().remove(serviceName);
		if (null != state) {
			try {
				var r = new UnSubscribe();
				r.Argument = state.subscribeInfo;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (RuntimeException e) {
				getSubscribeStates().putIfAbsent(serviceName, state); // rollback
				throw e;
			}
		}
	}

	private int ProcessNotifyServiceList(Protocol p) {
		var r = p instanceof NotifyServiceList ? (NotifyServiceList) p : null;
		var state = getSubscribeStates().get(r.Argument.getServiceName());
		if (null != state) {
			state.OnNotify(r.Argument);
		} else {
			Agent.logger.warn("NotifyServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private int ProcessCommitServiceList(Protocol p) {
		var r = p instanceof CommitServiceList ? (CommitServiceList) p : null;
		var state = getSubscribeStates().get(r.Argument.getServiceName());
		if (null != state) {
			state.OnCommit(r.Argument);
		} else {
			Agent.logger.warn("CommitServiceList But SubscribeState Not Found.");
		}
		return Procedure.Success;
	}

	private int ProcessKeepalive(Protocol p) {
		var r = p instanceof Keepalive ? (Keepalive) p : null;
		if (getOnKeepAlive() != null) {
			getOnKeepAlive().run();
		}
		r.SendResultCode(Keepalive.Success);
		return Procedure.Success;
	}

	private java.util.concurrent.ConcurrentHashMap<String, AutoKey> AutoKeys = new java.util.concurrent.ConcurrentHashMap<String, AutoKey>();

	private java.util.concurrent.ConcurrentHashMap<String, AutoKey> getAutoKeys() {
		return AutoKeys;
	}

	public AutoKey GetAutoKey(String name) {
		return getAutoKeys().computeIfAbsent(name, (k) -> new AutoKey(k, this));
	}

	public void Stop() {
		synchronized (this) {
			if (null == Client) {
				return;
			}
			Client.Stop();
			Client = null;
		}
	}

	public void OnConnected() {
		for (var e : Registers.entrySet()) {
			try {
				var r = new Register();
				r.Argument = e.getValue();
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (RuntimeException ex) {
				logger.debug("OnConnected.Register={}", e.getValue(), ex);
			}
		}
		for (var e : getSubscribeStates().entrySet()) {
			try {
				e.getValue().Committed = false;
				var r = new Subscribe();
				r.Argument = e.getValue().subscribeInfo;
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			} catch (RuntimeException ex) {
				logger.debug("OnConnected.Subscribe={}", e.getValue().subscribeInfo, ex);
			}
		}
	}

	/**
	 * 使用Config配置连接信息，可以配置是否支持重连。
	 * 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
	 */

	public final static String DefaultServiceName = "Zeze.Services.ServiceManager.Agent";
	public Agent(Zeze.Config config) {
		this(config, null);
	}

	public Agent(Zeze.Config config, String netServiceName) {
		if (null == config) {
			throw new RuntimeException("Config is null");
		}

		Client = (null == netServiceName || netServiceName.isEmpty()) ? new AgentClient(this, config)
				: new AgentClient(this, config, netServiceName);

		Client.AddFactoryHandle((new Register()).getTypeId(),
				new ProtocolFactoryHandle(() -> new Register()));

		Client.AddFactoryHandle((new UnRegister()).getTypeId(),
				new ProtocolFactoryHandle(() -> new UnRegister()));

		Client.AddFactoryHandle((new Subscribe()).getTypeId(),
				new ProtocolFactoryHandle(() -> new Subscribe()));

		Client.AddFactoryHandle((new UnSubscribe()).getTypeId(),
				new ProtocolFactoryHandle(() -> new UnSubscribe()));

		Client.AddFactoryHandle((new NotifyServiceList()).getTypeId(),
				new ProtocolFactoryHandle(() -> new NotifyServiceList(), (p) -> ProcessNotifyServiceList(p)));

		Client.AddFactoryHandle((new CommitServiceList()).getTypeId(),
				new ProtocolFactoryHandle(() -> new CommitServiceList(), (p) -> ProcessCommitServiceList(p)));

		Client.AddFactoryHandle((new Keepalive()).getTypeId(),
				new ProtocolFactoryHandle(() -> new Keepalive(), (p) -> ProcessKeepalive(p)));

		Client.AddFactoryHandle((new SubscribeFirstCommit()).getTypeId(), new Service.ProtocolFactoryHandle(
				() -> new SubscribeFirstCommit(), (p) -> ProcessSubscribeFirstCommit(p)));

		Client.AddFactoryHandle((new AllocateId()).getTypeId(),
				new ProtocolFactoryHandle(() -> new AllocateId()));

		Client.Start();
	}

	public void close() throws IOException {
		Stop();
	}
}
