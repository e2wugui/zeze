package Zeze.Services;

import RocksDbSharp.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public final class ServiceManager implements Closeable {
	//////////////////////////////////////////////////////////////////////////////
	/** 服务管理：注册和订阅
	 【名词】
	 动态服务(gs)
		 动态服务器一般指启用cache-sync的逻辑服务器。比如gs。
	 注册服务器（ServiceManager）
		 支持更新服务器，这个服务一开始是为了启用cache-sync的服务器的查找。
	 动态服务器列表使用者(linkd)
		 当前使用动态服务的客户端主要是Game2/linkd，linkd在hash分配请求的时候需要一致动态服务器列表。
	
	 【下面的流程都是用现有的服务名字（上面括号中的名字）】
	 
	 【本控制功能的目标】
	 所有的linkd的可用动态服务列表的更新并不是原子的。
	 1. 让所有的linkd的列表保持最新；
	 2. 尽可能减少linkd上的服务列表不一致的时间（通过ready-commit机制）；
	 3. 列表不一致时，分发请求可能引起cache不命中，但不影响正确性（cache-sync保证了正确性）；
	 
	 【主要事件和流程】
	 1. gs停止时调用 RegisterService,UnRegisterService 向ServiceManager声明自己服务状态。
	 2. linkd启动时调用 UseService, UnUseService 向ServiceManager申请使用gs-list。
	 3. ServiceManager在RegisterService,UnRegisterService处理时发送 NotifyServiceList 给所有的 linkd。
	 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
		当列表中的所有serivce都准备完成时调用 ReadyServiceList。
	 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
	 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
	 
	 【特别规则和错误处理】
	 1. linkd 异常停止，ServiceManager 按 UnUseService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
	 2. gs 异常停止，ServiceManager 按 UnRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
	 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
		linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
		linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
	 4. ServiceManager 异常关闭：
		a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的RegisterService一段时间,
		   然后开启新的一轮NotifyServiceList，等待时间内没有再次注册的gs以后当作新的处理。
		b) 启用raft的好处是raft的非master服务器会识别这种状态，并重定向请求到master，使得系统内只有一个master启用服务。
		   实际上raft不需要维护相同数据状态（gs-list），从空的开始即可，启用raft的话仅使用他的选举功能。
		#) 由于ServiceManager可以较快恢复，暂时不考虑使用Raft，实现无聊了再来加这个吧
	 5. ServiceManager开启一轮变更通告过程中，有新的gs启动停止，将开启新的通告(NotifyServiceList)。
		ReadyServiceList时会检查ready中的列表是否和当前ServiceManagerlist一致，不一致直接忽略。
		新的通告流程会促使linkd继续发送ready。
		另外为了更健壮的处理通告，通告加一个超时机制。超时没有全部ready，就启动一次新的通告。
		原则是：总按最新的gs-list通告。中间不一致的ready全部忽略。
	*/

	// ServiceInfo.Name -> ServiceState
	private java.util.concurrent.ConcurrentHashMap<String, ServerState> ServerStates = new java.util.concurrent.ConcurrentHashMap<String, ServerState>();
	private NetServer Server;
	public NetServer getServer() {
		return Server;
	}
	private void setServer(NetServer value) {
		Server = value;
	}
	private AsyncSocket ServerSocket;
	private volatile Util.SchedulerTask StartNotifyDelayTask;

	public final static class Conf implements Zeze.Config.ICustomize {
		public String getName() {
			return "Zeze.Services.ServiceManager";
		}

		private int KeepAlivePeriod = 300 * 1000;
		public int getKeepAlivePeriod() {
			return KeepAlivePeriod;
		}
		public void setKeepAlivePeriod(int value) {
			KeepAlivePeriod = value;
		}

		/** 
		 启动以后接收注册和订阅，一段时间内不进行通知。
		 用来处理ServiceManager异常重启导致服务列表重置的问题。
		 在Delay时间内，希望所有的服务都重新连接上来并注册和订阅。
		 Delay到达时，全部通知一遍，以后正常工作。
		*/
		private int StartNotifyDelay = 12 * 1000;
		public int getStartNotifyDelay() {
			return StartNotifyDelay;
		}
		public void setStartNotifyDelay(int value) {
			StartNotifyDelay = value;
		}

		private int RetryNotifyDelayWhenNotAllReady = 30 * 1000;
		public int getRetryNotifyDelayWhenNotAllReady() {
			return RetryNotifyDelayWhenNotAllReady;
		}
		public void setRetryNotifyDelayWhenNotAllReady(int value) {
			RetryNotifyDelayWhenNotAllReady = value;
		}
		private String DbHome = ".";
		public String getDbHome() {
			return DbHome;
		}
		private void setDbHome(String value) {
			DbHome = value;
		}

		public void Parse(XmlElement self) {
			String attr = self.GetAttribute("KeepAlivePeriod");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setKeepAlivePeriod(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("StartNotifyDelay");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setStartNotifyDelay(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setRetryNotifyDelayWhenNotAllReady(Integer.parseInt(attr));
			}
			setDbHome(self.GetAttribute("DbHome"));
			if (tangible.StringHelper.isNullOrEmpty(getDbHome())) {
				setDbHome(".");
			}
		}
	}

	/** 
	 需要从配置文件中读取，把这个引用加入： Zeze.Config.AddCustomize
	*/
	private Conf Config = new Conf();
	public Conf getConfig() {
		return Config;
	}

	public final static class ServerState {
		private ServiceManager ServiceManager;
		public ServiceManager getServiceManager() {
			return ServiceManager;
		}
		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}

		// identity ->
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private java.util.concurrent.ConcurrentHashMap<String, ServiceInfo> ServiceInfos = new java.util.concurrent.ConcurrentHashMap<String, ServiceInfo> ();
		public java.util.concurrent.ConcurrentHashMap<String, ServiceInfo> getServiceInfos() {
			return ServiceInfos;
		}
		private java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> Simple = new java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> ();
		public java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> getSimple() {
			return Simple;
		}
		private java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> ReadyCommit = new java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> ();
		public java.util.concurrent.ConcurrentHashMap<Long, SubscribeState> getReadyCommit() {
			return ReadyCommit;
		}

		private Zeze.Util.SchedulerTask NotifyTimeoutTask;
		private long SerialId;

		public ServerState(ServiceManager sm, String serviceName) {
			ServiceManager = sm;
			ServiceName = serviceName;
		}

		public void Close() {
			if (NotifyTimeoutTask != null) {
				NotifyTimeoutTask.Cancel();
			}
			NotifyTimeoutTask = null;
		}

		public void StartNotify() {
			synchronized (this) {
				if (null != getServiceManager().StartNotifyDelayTask) {
					return;
				}
				var notify = new NotifyServiceList();
				notify.setArgument(new ServiceManager.ServiceInfos(getServiceName(), this, ++SerialId));
				logger.Debug("StartNotify {0}", notify.getArgument());
				var notifyBytes = notify.Encode();

				for (var e : getSimple()) {
					if (getServiceManager().getServer().GetSocket(e.Key) != null) {
						getServiceManager().getServer().GetSocket(e.Key).Send(notifyBytes);
					}
				}
				for (var e : getReadyCommit()) {
					e.Value.Ready = false;
					if (getServiceManager().getServer().GetSocket(e.Key) != null) {
						getServiceManager().getServer().GetSocket(e.Key).Send(notifyBytes);
					}
				}
				if (!getReadyCommit().isEmpty()) {
					// 只有两段公告模式需要回应处理。
					NotifyTimeoutTask = Zeze.Util.Scheduler.getInstance().Schedule((ThisTask) -> {
								if (NotifyTimeoutTask == ThisTask) {
									// NotifyTimeoutTask 会在下面两种情况下被修改：
									// 1. 在 Notify.ReadyCommit 完成以后会被清空。
									// 2. 启动了新的 Notify。
									StartNotify(); // restart
								}
					}, getServiceManager().getConfig().getRetryNotifyDelayWhenNotAllReady(), -1);
				}
			}
		}

		public void TryCommit() {
			synchronized (this) {
				if (NotifyTimeoutTask == null) {
					return; // no pending notify
				}

				for (var e : getReadyCommit()) {
					if (false == e.Value.Ready) {
						return;
					}
				}
				var commit = new CommitServiceList();
				commit.setArgument(new ServiceInfos(getServiceName(), this, 0));
				for (var e : getReadyCommit()) {
					if (getServiceManager().getServer().GetSocket(e.Key) != null) {
						getServiceManager().getServer().GetSocket(e.Key).Send(commit);
					}
				}
				if (NotifyTimeoutTask != null) {
					NotifyTimeoutTask.Cancel();
				}
				NotifyTimeoutTask = null;
			}
		}

		/** 
		 订阅时候返回的ServiceInfos，必须和Notify流程互斥。
		 原子的得到当前信息并发送，然后加入订阅(simple or readycommit)。
		*/
		public int SubscribeAndSend(Subscribe r, Session session) {
			synchronized (this) {
				// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
				switch (r.getArgument().getSubscribeType()) {
					case SubscribeInfo.SubscribeTypeSimple:
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
						getSimple().TryAdd(session.getSessionId(), new SubscribeState(session.getSessionId()));
						break;
					case SubscribeInfo.SubscribeTypeReadyCommit:
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
						getReadyCommit().TryAdd(session.getSessionId(), new SubscribeState(session.getSessionId()));
						break;
					default:
						r.setResultCode(Subscribe.UnknownSubscribeType);
						r.SendResult();
						return Procedure.LogicError;
				}
				r.SendResultCode(Subscribe.Success);
				if (null == getServiceManager().StartNotifyDelayTask) {
					var arg = new ServiceInfos(getServiceName(), this, ++SerialId);
					SubscribeFirstCommit tempVar = new SubscribeFirstCommit();
					tempVar.setArgument(arg);
					tempVar.Send(r.getSender());
				}
				return Procedure.Success;
			}
		}

		public void SetReady(ReadyServiceList p, Session session) {
			synchronized (this) {
				if (p.getArgument().getSerialId() != SerialId) {
					logger.Debug("Skip Ready: SerialId Not Equal.");
					return;
				}
				var ordered = new ServiceInfos(getServiceName(), this, 0);

				// 忽略旧的Ready。
				if (!Enumerable.SequenceEqual(ordered.getServiceInfoListSortedByIdentity(), p.getArgument().getServiceInfoListSortedByIdentity())) {
					var sb = new StringBuilder();
					sb.append("SequenceNotEqual:");
					sb.append(" Current=").append(ordered);
					sb.append(" Ready=").append(p.getArgument());
					logger.Debug(sb.toString());
					return;
				}

				TValue subcribeState;
				tangible.OutObject<TValue> tempOut_subcribeState = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				if (!getReadyCommit().TryGetValue(session.getSessionId(), tempOut_subcribeState)) {
				subcribeState = tempOut_subcribeState.outArgValue;
					return;
				}
			else {
				subcribeState = tempOut_subcribeState.outArgValue;
			}

				subcribeState.Ready = true;
				TryCommit();
			}
		}
	}

	public final static class SubscribeState {
		private long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private boolean Ready;
		public boolean getReady() {
			return Ready;
		}
		public void setReady(boolean value) {
			Ready = value;
		}
		public SubscribeState(long ssid) {
			SessionId = ssid;
		}
	}

	public final static class Session {
		private ServiceManager ServiceManager;
		public ServiceManager getServiceManager() {
			return ServiceManager;
		}
		private long SessionId;
		public long getSessionId() {
			return SessionId;
		}
		private java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> (new ServiceInfoEqualityComparer());
		public java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> getRegisters() {
			return Registers;
		}
		// key is ServiceName: 会话订阅
		private java.util.concurrent.ConcurrentHashMap<String, SubscribeInfo> Subscribes = new java.util.concurrent.ConcurrentHashMap<String, SubscribeInfo> ();
		public java.util.concurrent.ConcurrentHashMap<String, SubscribeInfo> getSubscribes() {
			return Subscribes;
		}
		private Util.SchedulerTask KeepAliveTimerTask;

		public Session(ServiceManager sm, long ssid) {
			ServiceManager = sm;
			SessionId = ssid;
			KeepAliveTimerTask = Util.Scheduler.getInstance().Schedule((ThisTask) -> {
						try {
							var r = new Keepalive();
							var s = getServiceManager().getServer().GetSocket(getSessionId());
							r.SendAndWaitCheckResultCode(s);
						}
						catch (RuntimeException ex) {
							if (getServiceManager().getServer().GetSocket(getSessionId()) != null) {
								getServiceManager().getServer().GetSocket(getSessionId()).Dispose();
							}
							logger.Error(ex, "ServiceManager.KeepAlive");
						}
			}, Util.Random.getInstance().nextInt(getServiceManager().getConfig().getKeepAlivePeriod()), getServiceManager().getConfig().getKeepAlivePeriod());
		}

		public void OnClose() {
			if (KeepAliveTimerTask != null) {
				KeepAliveTimerTask.Cancel();
			}
			KeepAliveTimerTask = null;

			for (var info : getSubscribes().values()) {
				getServiceManager().UnSubscribeNow(getSessionId(), info);
			}

			HashMap<String, ServerState> changed = new HashMap<String, ServerState>(getRegisters().size());

			for (var info : getRegisters().values()) {
				var state = getServiceManager().UnRegisterNow(getSessionId(), info);
				if (null != state) {
					changed.TryAdd(state.getServiceName(), state);
				}
			}

			for (var state : changed.values()) {
				state.StartNotify();
			}
		}
	}

	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private int ProcessRegister(Protocol p) {
		var r = p instanceof Register ? (Register)p : null;
		Object tempVar = r.getSender().getUserState();
		var session = tempVar instanceof Session ? (Session)tempVar : null;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (false == session.getRegisters().TryAdd(r.getArgument(), r.getArgument())) {
			r.SendResultCode(Register.DuplicateRegister);
			return Procedure.LogicError;
		}
		var state = ServerStates.putIfAbsent(r.getArgument().getServiceName(), (name) -> new ServerState(this, name));

		// 【警告】
		// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
		// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
		// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
		// 另外它也被Session引用（用于连接关闭时，自动注销）。
		// 这是专用程序，不是一个库，以后有修改时，小心就是了。
		r.getArgument().setLocalState(r.getSender().getSessionId());

		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		state.ServiceInfos.AddOrUpdate(r.getArgument().getServiceIdentity(), r.getArgument(), (key, value) -> r.getArgument());
		r.SendResultCode(Register.Success);
		state.StartNotify();
		return Procedure.Success;
	}

	public ServerState UnRegisterNow(long sessionId, ServiceInfo info) {
		TValue state;
		tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (ServerStates.TryGetValue(info.getServiceName(), tempOut_state)) {
		state = tempOut_state.outArgValue;
			Object exist;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (state.ServiceInfos.TryGetValue(info.getServiceIdentity(), out exist)) {
				// 这里存在一个时间窗口，可能使得重复的注销会成功。注销一般比较特殊，忽略这个问题。
				Long existSessionId = exist.LocalState instanceof Long ? (Long)exist.LocalState : null;
				if (existSessionId == null || sessionId == existSessionId.longValue()) {
					// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
					Object _;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
					state.ServiceInfos.TryRemove(info.getServiceIdentity(), out _);
					return state;
				}
			}
		}
	else {
		state = tempOut_state.outArgValue;
	}
		return null;
	}

	private int ProcessUnRegister(Protocol p) {
		var r = p instanceof UnRegister ? (UnRegister)p : null;
		Object tempVar = r.getSender().getUserState();
		var session = tempVar instanceof Session ? (Session)tempVar : null;
		if (null != UnRegisterNow(r.getSender().getSessionId(), r.getArgument())) {
			// ignore TryRemove failed.
			TValue _;
			tangible.OutObject<ServiceInfo> tempOut__ = new tangible.OutObject<ServiceInfo>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			session.getRegisters().TryRemove(r.getArgument(), tempOut__);
		_ = tempOut__.outArgValue;
			//r.SendResultCode(UnRegister.Success);
			//return Procedure.Success;
		}
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(UnRegister.Success);
		return Procedure.Success;
	}

	private int ProcessSubscribe(Protocol p) {
		var r = p instanceof Subscribe ? (Subscribe)p : null;
		Object tempVar = r.getSender().getUserState();
		var session = tempVar instanceof Session ? (Session)tempVar : null;
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (!session.getSubscribes().TryAdd(r.getArgument().getServiceName(), r.getArgument())) {
			r.setResultCode(Subscribe.DuplicateSubscribe);
			r.SendResult();
			return Procedure.LogicError;
		}
		var state = ServerStates.putIfAbsent(r.getArgument().getServiceName(), (name) -> new ServerState(this, name));
		return state.SubscribeAndSend(r, session);
	}

	public ServerState UnSubscribeNow(long sessionId, SubscribeInfo info) {
		TValue state;
		tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (ServerStates.TryGetValue(info.getServiceName(), tempOut_state)) {
		state = tempOut_state.outArgValue;
			switch (info.getSubscribeType()) {
				case SubscribeInfo.SubscribeTypeSimple:
					Object _;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
					if (state.Simple.TryRemove(sessionId, out _)) {
						return state;
					}
					break;
				case SubscribeInfo.SubscribeTypeReadyCommit:
					Object _;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
					if (state.ReadyCommit.TryRemove(sessionId, out _)) {
						return state;
					}
					break;
			}
		}
	else {
		state = tempOut_state.outArgValue;
	}
		return null;
	}

	private int ProcessUnSubscribe(Protocol p) {
		var r = p instanceof UnSubscribe ? (UnSubscribe)p : null;
		Object tempVar = r.getSender().getUserState();
		var session = tempVar instanceof Session ? (Session)tempVar : null;
		TValue sub;
		tangible.OutObject<SubscribeInfo> tempOut_sub = new tangible.OutObject<SubscribeInfo>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (session.getSubscribes().TryRemove(r.getArgument().getServiceName(), tempOut_sub)) {
		sub = tempOut_sub.outArgValue;
			if (r.getArgument().getSubscribeType() == sub.SubscribeType) {
				var changed = UnSubscribeNow(r.getSender().getSessionId(), r.getArgument());
				if (null != changed) {
					r.setResultCode(UnSubscribe.Success);
					r.SendResult();
					changed.TryCommit();
					return Procedure.Success;
				}
			}
		}
	else {
		sub = tempOut_sub.outArgValue;
	}
		// 取消订阅不能存在返回成功。否则Agent比较麻烦。
		//r.ResultCode = UnSubscribe.NotExist;
		//r.SendResult();
		//return Procedure.LogicError;
		r.setResultCode(UnRegister.Success);
		r.SendResult();
		return Procedure.Success;
	}

	private int ProcessReadyServiceList(Protocol p) {
		var r = p instanceof ReadyServiceList ? (ReadyServiceList)p : null;
		Object tempVar = r.getSender().getUserState();
		var session = tempVar instanceof Session ? (Session)tempVar : null;
		var state = ServerStates.putIfAbsent(r.getArgument().getServiceName(), (name) -> new ServerState(this, name));
		if (state != null) {
			state.SetReady(r, session);
		}
		return Procedure.Success;
	}

	public void close() throws IOException {
		Stop();
	}


	public ServiceManager(IPAddress ipaddress, int port, Config config) {
		this(ipaddress, port, config, -1);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ServiceManager(IPAddress ipaddress, int port, Config config, int startNotifyDelay = -1)
	public ServiceManager(IPAddress ipaddress, int port, Config config, int startNotifyDelay) {
		T tmpconf;
		tangible.OutObject<Conf> tempOut_tmpconf = new tangible.OutObject<Conf>();
		if (config.<Conf>GetCustomize(tempOut_tmpconf)) {
		tmpconf = tempOut_tmpconf.outArgValue;
			Config = tmpconf;
		}
	else {
		tmpconf = tempOut_tmpconf.outArgValue;
	}

		if (startNotifyDelay >= 0) {
			getConfig().setStartNotifyDelay(startNotifyDelay);
		}

		setServer(new NetServer(this, config));

		getServer().AddFactoryHandle((new Register()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Register(), Handle = ProcessRegister});

		getServer().AddFactoryHandle((new UnRegister()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnRegister(), Handle = ProcessUnRegister});

		getServer().AddFactoryHandle((new Subscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Subscribe(), Handle = ProcessSubscribe});

		getServer().AddFactoryHandle((new UnSubscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnSubscribe(), Handle = ProcessUnSubscribe});

		getServer().AddFactoryHandle((new ReadyServiceList()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new ReadyServiceList(), Handle = ProcessReadyServiceList});

		getServer().AddFactoryHandle((new Keepalive()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Keepalive()});

		getServer().AddFactoryHandle((new AllocateId()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new AllocateId(), Handle = ProcessAllocateId});

		if (getConfig().getStartNotifyDelay() > 0) {
			StartNotifyDelayTask = Util.Scheduler.getInstance().Schedule(::StartNotifyAll, getConfig().getStartNotifyDelay(), -1);
		}

		var options = (new DbOptions()).SetCreateIfMissing(true);
		AutoKeysDb = RocksDb.Open(options, Paths.get(getConfig().getDbHome()).resolve("autokeys").toString());

		// 允许配置多个acceptor，如果有冲突，通过日志查看。
		ServerSocket = getServer().NewServerSocket(ipaddress, port);
		getServer().Start();
	}

	private RocksDb AutoKeysDb;
	private java.util.concurrent.ConcurrentHashMap<String, AutoKey> AutoKeys = new java.util.concurrent.ConcurrentHashMap<String, AutoKey> ();
	private java.util.concurrent.ConcurrentHashMap<String, AutoKey> getAutoKeys() {
		return AutoKeys;
	}

	public final static class AutoKey {
		private String Name;
		public String getName() {
			return Name;
		}
		private RocksDb Db;
		public RocksDb getDb() {
			return Db;
		}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] Key;
		private byte[] Key;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] getKey()
		private byte[] getKey() {
			return Key;
		}
		private long Current;
		private long getCurrent() {
			return Current;
		}
		private void setCurrent(long value) {
			Current = value;
		}

		public AutoKey(String name, RocksDbSharp.RocksDb db) {
			Name = name;
			Db = db; {
				var bb = ByteBuffer.Allocate();
				bb.WriteString(getName());
				Key = bb.Copy();
			}
			var value = getDb().Get(getKey());
			if (null != value) {
				var bb = ByteBuffer.Wrap(value);
				setCurrent(bb.ReadLong());
			}
		}

		public void Allocate(AllocateId rpc) {
			synchronized (this) {
				rpc.getResult().setStartId(getCurrent());

				var count = rpc.getArgument().getCount();

				// 随便修正一下分配数量。
				if (count < 256) {
					count = 256;
				}
				else if (count > 10000) {
					count = 10000;
				}

				setCurrent(getCurrent() + count);
				var bb = ByteBuffer.Allocate();
				bb.WriteLong(getCurrent());
				getDb().Put(getKey(), getKey().length, bb.getBytes(), bb.getSize(), null, (new WriteOptions()).SetSync(true));

				rpc.getResult().setCount(count);
			}
		}
	}

	private int ProcessAllocateId(Protocol p) {
		var r = p instanceof AllocateId ? (AllocateId)p : null;
		var n = r.getArgument().getName();
		r.getResult().Name = n;
		getAutoKeys().putIfAbsent(n, (_) -> new AutoKey(n, AutoKeysDb)).Allocate(r);
		r.SendResult();
		return 0;
	}

	private void StartNotifyAll(Util.SchedulerTask ThisTask) {
		StartNotifyDelayTask = null;
		for (var e : ServerStates) {
			e.Value.StartNotify();
		}
	}

	public void Stop() {
		synchronized (this) {
			if (null == getServer()) {
				return;
			}
			if (StartNotifyDelayTask != null) {
				StartNotifyDelayTask.Cancel();
			}
			ServerSocket.close();
			ServerSocket = null;
			getServer().Stop();
			setServer(null);

			for (var ss : ServerStates.values()) {
				ss.Close();
			}
			if (AutoKeysDb != null) {
				AutoKeysDb.Dispose();
			}
		}
	}

	public final static class NetServer extends HandshakeServer {
		private ServiceManager ServiceManager;
		public ServiceManager getServiceManager() {
			return ServiceManager;
		}

		public NetServer(ServiceManager sm, Config config) {
			super("Zeze.Services.ServiceManager", config);
			ServiceManager = sm;
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) {
			so.setUserState(new Session(getServiceManager(), so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) {
			Object tempVar = so.getUserState();
			var session = tempVar instanceof Session ? (Session)tempVar : null;
			if (session != null) {
				session.OnClose();
			}
			super.OnSocketClose(so, e);
		}
	}

	public final static class ServiceInfo extends Zeze.Transaction.Bean {
		/** 
		 服务名，比如"GameServer"
		*/
		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		private void setServiceName(String value) {
			ServiceName = value;
		}

		/** 
		 服务id，对于 Zeze.Application，一般就是 Config.AutoKeyLocalId.
		 这里使用类型 string 是为了更好的支持扩展。
		*/
		private String ServiceIdentity;
		public String getServiceIdentity() {
			return ServiceIdentity;
		}
		private void setServiceIdentity(String value) {
			ServiceIdentity = value;
		}

		/** 
		 服务ip-port，如果没有，保持空和0.
		*/
		private String PassiveIp = "";
		public String getPassiveIp() {
			return PassiveIp;
		}
		private void setPassiveIp(String value) {
			PassiveIp = value;
		}
		private int PassivePort = 0;
		public int getPassivePort() {
			return PassivePort;
		}
		private void setPassivePort(int value) {
			PassivePort = value;
		}

		// 服务扩展信息，可选。
		private Binary ExtraInfo = Binary.Empty;
		public Binary getExtraInfo() {
			return ExtraInfo;
		}
		private void setExtraInfo(Binary value) {
			ExtraInfo = value;
		}

		// ServiceManager或者ServiceManager.Agent用来保存本地状态，不是协议一部分，不会被系列化。
		// 算是一个简单的策略，不怎么优美。一般仅设置一次，线程保护由使用者自己管理。
		private Object LocalState;
		public Object getLocalState() {
			return LocalState;
		}
		public void setLocalState(Object value) {
			LocalState = value;
		}

		public ServiceInfo() {
		}


		public ServiceInfo(String name, String identity, String ip, int port) {
			this(name, identity, ip, port, null);
		}

		public ServiceInfo(String name, String identity, String ip) {
			this(name, identity, ip, 0, null);
		}

		public ServiceInfo(String name, String identity) {
			this(name, identity, null, 0, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ServiceInfo(string name, string identity, string ip = null, int port = 0, Binary extrainfo = null)
		public ServiceInfo(String name, String identity, String ip, int port, Binary extrainfo) {
			setServiceName(name);
			setServiceIdentity(identity);
			if (!ip.equals(null)) {
				setPassiveIp(ip);
			}
			setPassivePort(port);
			if (extrainfo != null) {
				setExtraInfo(extrainfo);
			}
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			setServiceIdentity(bb.ReadString());
			setPassiveIp(bb.ReadString());
			setPassivePort(bb.ReadInt());
			setExtraInfo(bb.ReadBinary());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteString(getServiceIdentity());
			bb.WriteString(getPassiveIp());
			bb.WriteInt(getPassivePort());
			bb.WriteBinary(getExtraInfo());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 17;
			result = prime * result + getServiceName().hashCode();
			result = prime * result + getServiceIdentity().hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			boolean tempVar = obj instanceof ServiceInfo;
			ServiceInfo other = tempVar ? (ServiceInfo)obj : null;
			if (tempVar) {
				return getServiceName().equals(other.getServiceName()) && getServiceIdentity().equals(other.getServiceIdentity());
			}
			return false;
		}
	}

	/** 
	 动态服务启动时通过这个rpc注册自己。
	*/
	public final static class Register extends Rpc<ServiceInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Register.class.FullName);

		public static final int Success = 0;
		public static final int DuplicateRegister = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

	}

	/** 
	 动态服务关闭时，注销自己，当与本服务器的连接关闭时，默认也会注销。
	 最好主动注销，方便以后错误处理。
	*/
	public final static class UnRegister extends Rpc<ServiceInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(UnRegister.class.FullName);

		public static final int Success = 0;
		public static final int NotExist = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class SubscribeInfo extends Bean {
		public static final int SubscribeTypeSimple = 0;
		public static final int SubscribeTypeReadyCommit = 1;

		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		public void setServiceName(String value) {
			ServiceName = value;
		}
		private int SubscribeType;
		public int getSubscribeType() {
			return SubscribeType;
		}
		public void setSubscribeType(int value) {
			SubscribeType = value;
		}
		private Object LocalState;
		public Object getLocalState() {
			return LocalState;
		}
		public void setLocalState(Object value) {
			LocalState = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			setSubscribeType(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteInt(getSubscribeType());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return String.format("%1$s:%2$s", getServiceName(), getSubscribeType());
		}
	}

	public final static class Subscribe extends Rpc<SubscribeInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Subscribe.class.FullName);

		public static final int Success = 0;
		public static final int DuplicateSubscribe = 1;
		public static final int UnknownSubscribeType = 2;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class UnSubscribe extends Rpc<SubscribeInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(UnSubscribe.class.FullName);

		public static final int Success = 0;
		public static final int NotExist = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ServiceInfos extends Bean {
		// ServiceList maybe empty. need a ServiceName
		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		private void setServiceName(String value) {
			ServiceName = value;
		}
		// sorted by ServiceIdentity
		private ArrayList<ServiceInfo> _ServiceInfoListSortedByIdentity = new ArrayList<ServiceInfo> ();
		private ArrayList<ServiceInfo> getServiceInfoListSortedByIdentity() {
			return _ServiceInfoListSortedByIdentity;
		}
		public IReadOnlyList<ServiceInfo> getServiceInfoListSortedByIdentity() {
			return getServiceInfoListSortedByIdentity();
		}
		private long SerialId;
		public long getSerialId() {
			return SerialId;
		}
		public void setSerialId(long value) {
			SerialId = value;
		}

		public ServiceInfos() {
		}

		public ServiceInfos(String serviceName) {
			setServiceName(serviceName);
		}

		public ServiceInfos(String serviceName, ServerState state, long serialId) {
			setServiceName(serviceName);
			for (var e : state.getServiceInfos()) {
				getServiceInfoListSortedByIdentity().add(e.Value);
			}
			setSerialId(serialId);
		}

		public boolean TryGetServiceInfo(String identity, tangible.OutObject<ServiceInfo> info) {
			var cur = new ServiceInfo(getServiceName(), identity);
			int index = getServiceInfoListSortedByIdentity().BinarySearch(cur, new ServiceInfoIdentityComparer());
			if (index >= 0) {
				info.outArgValue = getServiceInfoListSortedByIdentity().get(index);
				return true;
			}
			info.outArgValue = null;
			return false;
		}
		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			getServiceInfoListSortedByIdentity().clear();
			for (int c = bb.ReadInt(); c > 0; --c) {
				var service = new ServiceInfo();
				service.Decode(bb);
				getServiceInfoListSortedByIdentity().add(service);
			}
			setSerialId(bb.ReadLong());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteInt(getServiceInfoListSortedByIdentity().size());
			for (var service : getServiceInfoListSortedByIdentity()) {
				service.Encode(bb);
			}
			bb.WriteLong(getSerialId());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append(getServiceName()).append("=");
			sb.append("[");
			for (var e : getServiceInfoListSortedByIdentity()) {
				sb.append(e.getServiceIdentity());
				sb.append(",");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public final static class NotifyServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(NotifyServiceList.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ReadyServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(ReadyServiceList.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class CommitServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(CommitServiceList.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	// 实际上可以不用这个类，为了保持以后ServiceInfo的比较可能改变，写一个这个类。
	public final static class ServiceInfoEqualityComparer implements IEqualityComparer<ServiceInfo> {
		public boolean equals(ServiceInfo x, ServiceInfo y) {
			return x.equals(y);
		}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: public int GetHashCode([DisallowNull] ServiceInfo obj)
		public int hashCode(ServiceInfo obj) {
			return obj.hashCode();
		}
	}

	public final static class ServiceInfoIdentityComparer implements Comparator<ServiceInfo> {
		public int compare(ServiceInfo x, ServiceInfo y) {
//C# TO JAVA CONVERTER TODO TASK: The following System.String compare method is not converted:
			return x.getServiceIdentity().CompareTo(y.getServiceIdentity());
		}
	}

	public final static class Keepalive extends Rpc<EmptyBean, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Keepalive.class.FullName);

		public static final int Success = 0;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class SubscribeFirstCommit extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(SubscribeFirstCommit.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class AllocateIdArgument extends Bean {
		private String Name;
		public String getName() {
			return Name;
		}
		public void setName(String value) {
			Name = value;
		}
		private int Count;
		public int getCount() {
			return Count;
		}
		public void setCount(int value) {
			Count = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setCount(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteInt(getCount());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class AllocateIdResult extends Bean {
		private String Name;
		public String getName() {
			return Name;
		}
		public void setName(String value) {
			Name = value;
		}
		private long StartId;
		public long getStartId() {
			return StartId;
		}
		public void setStartId(long value) {
			StartId = value;
		}
		private int Count;
		public int getCount() {
			return Count;
		}
		public void setCount(int value) {
			Count = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setStartId(bb.ReadLong());
			setCount(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteLong(getStartId());
			bb.WriteInt(getCount());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class AllocateId extends Rpc<AllocateIdArgument, AllocateIdResult> {
		public final static int ProtocolId_ = Bean.Hash16(AllocateId.class.FullName);

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class Agent implements Closeable {
		// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
		// ServiceName ->
		private java.util.concurrent.ConcurrentHashMap<String, SubscribeState> SubscribeStates = new java.util.concurrent.ConcurrentHashMap<String, SubscribeState> ();
		public java.util.concurrent.ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
			return SubscribeStates;
		}
		private NetClient Client;
		public NetClient getClient() {
			return Client;
		}
		private void setClient(NetClient value) {
			Client = value;
		}

		/** 
		 订阅服务状态发生变化时回调。
		 如果需要处理这个事件，请在订阅前设置回调。
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

		// key is (ServiceName, ServideIdentity)
		private java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> (new ServiceInfoEqualityComparer());
		private java.util.concurrent.ConcurrentHashMap<ServiceInfo, ServiceInfo> getRegisters() {
			return Registers;
		}

		// 【警告】
		// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
		// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
		// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
		public final static class SubscribeState {
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}
			private SubscribeInfo SubscribeInfo;
			public SubscribeInfo getSubscribeInfo() {
				return SubscribeInfo;
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
			 刚初始化时为false，任何修改ServiceInfos都会设置成true。
			 用来处理Subscribe返回的第一份数据和Commit可能乱序的问题。
			 目前的实现不会发生乱序。
			*/
			private boolean Committed = false;
			public boolean getCommitted() {
				return Committed;
			}
			public void setCommitted(boolean value) {
				Committed = value;
			}

			// 服务准备好。
			private java.util.concurrent.ConcurrentHashMap<String, Object> ServiceIdentityReadyStates = new java.util.concurrent.ConcurrentHashMap<String, Object> ();
			public java.util.concurrent.ConcurrentHashMap<String, Object> getServiceIdentityReadyStates() {
				return ServiceIdentityReadyStates;
			}

			public SubscribeState(Agent ag, SubscribeInfo info) {
				Agent = ag;
				SubscribeInfo = info;
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
				r.setArgument(getServiceInfosPending());
				if (getAgent().getClient().Socket != null) {
					getAgent().getClient().Socket.Send(r);
				}
				return true;
			}

			public void SetServiceIdentityReadyState(String identity, Object state) {
				if (null == state) {
					TValue _;
					tangible.OutObject<Object> tempOut__ = new tangible.OutObject<Object>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getServiceIdentityReadyStates().TryRemove(identity, tempOut__);
				_ = tempOut__.outArgValue;
				}
				else {
					getServiceIdentityReadyStates().put(identity, state);
				}

				synchronized (this) {
					// 把 state 复制到当前版本的服务列表中。允许列表不变，服务状态改变。
					Zeze.Services.ServiceManager.ServiceInfo info;
					tangible.OutObject<Zeze.Services.ServiceManager.ServiceInfo> tempOut_info = new tangible.OutObject<Zeze.Services.ServiceManager.ServiceInfo>();
					if (getServiceInfos() != null && getServiceInfos().TryGetServiceInfo(identity, tempOut_info)) {
					info = tempOut_info.outArgValue;
						info.setLocalState(state);
					}
				else {
					info = tempOut_info.outArgValue;
				}
					// 尝试发送Ready，如果有pending.
					TrySendReadyServiceList();
				}
			}

			private void PrepareAndTriggerOnchanged() {
				for (var info : getServiceInfos().getServiceInfoListSortedByIdentity()) {
					TValue state;
					tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					if (getServiceIdentityReadyStates().TryGetValue(info.getServiceIdentity(), tempOut_state)) {
					state = tempOut_state.outArgValue;
						info.setLocalState(state);
					}
				else {
					state = tempOut_state.outArgValue;
				}
				}
				if (getAgent().OnChanged != null) {
					getAgent().OnChanged.Invoke(this);
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
							if (null == getServiceInfosPending() || infos.getSerialId() > getServiceInfosPending().getSerialId()) {
								setServiceInfosPending(infos);
								TrySendReadyServiceList();
							}
							break;
					}
				}
			}

			public void OnCommit(ServiceInfos infos) {
				synchronized (this) {
					// ServiceInfosPending 和 Commit.infos 应该一样，否则肯定哪里出错了。
					// 这里总是使用最新的 Commit.infos，检查记录日志。
					if (!Enumerable.SequenceEqual(infos.getServiceInfoListSortedByIdentity(), getServiceInfosPending().getServiceInfoListSortedByIdentity())) {
						Agent.logger.Warn("OnCommit: ServiceInfosPending Miss Match.");
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

		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();


		public ServiceInfo RegisterService(String name, String identity, String ip, int port) {
			return RegisterService(name, identity, ip, port, null);
		}

		public ServiceInfo RegisterService(String name, String identity, String ip) {
			return RegisterService(name, identity, ip, 0, null);
		}

		public ServiceInfo RegisterService(String name, String identity) {
			return RegisterService(name, identity, null, 0, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ServiceInfo RegisterService(string name, string identity, string ip = null, int port = 0, Binary extrainfo = null)
		public ServiceInfo RegisterService(String name, String identity, String ip, int port, Binary extrainfo) {
			return RegisterService(new ServiceInfo(name, identity, ip, port, extrainfo));
		}

		public void WaitConnectorReady() {
			// 实际上只有一个连接，这样就不用查找了。
			getClient().getConfig().ForEachConnector((c) -> c.WaitReady());
		}

		private ServiceInfo RegisterService(ServiceInfo info) {
			WaitConnectorReady();

			boolean regNew = false;
			var regServInfo = getRegisters().putIfAbsent(info, (key) -> {
						regNew = true;
						return info;
			});

			if (regNew) {
				try {
					var r = new Register();
					r.setArgument(info);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getRegisters().TryRemove(KeyValuePair.Create(info, info)); // rollback
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

			TValue exist;
			tangible.OutObject<ServiceInfo> tempOut_exist = new tangible.OutObject<ServiceInfo>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getRegisters().TryRemove(info, tempOut_exist)) {
			exist = tempOut_exist.outArgValue;
				try {
					var r = new UnRegister();
					r.setArgument(info);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getRegisters().TryAdd(exist, exist); // rollback
					throw e;
				}
			}
		else {
			exist = tempOut_exist.outArgValue;
		}
		}


		public SubscribeState SubscribeService(String serviceName, int type) {
			return SubscribeService(serviceName, type, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public SubscribeState SubscribeService(string serviceName, int type, object state = null)
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

			boolean newAdd = false;
			var subState = getSubscribeStates().putIfAbsent(info.getServiceName(), (_) -> {
						newAdd = true;
						return new SubscribeState(this, info);
			});

			if (newAdd) {
				var r = new Subscribe();
				r.setArgument(info);
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			}
			return subState;
		}

		private int ProcessSubscribeFirstCommit(Protocol p) {
			var r = p instanceof SubscribeFirstCommit ? (SubscribeFirstCommit)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnFirstCommit(r.getArgument());
			}
		else {
			state = tempOut_state.outArgValue;
		}
			return Procedure.Success;
		}

		public void UnSubscribeService(String serviceName) {
			WaitConnectorReady();

			TValue state;
			tangible.OutObject<SubscribeState> tempOut_state = new tangible.OutObject<SubscribeState>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryRemove(serviceName, tempOut_state)) {
			state = tempOut_state.outArgValue;
				try {
					var r = new UnSubscribe();
					r.setArgument(state.SubscribeInfo);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getSubscribeStates().TryAdd(serviceName, state); // rollback
					throw e;
				}
			}
		else {
			state = tempOut_state.outArgValue;
		}
		}

		private int ProcessNotifyServiceList(Protocol p) {
			var r = p instanceof NotifyServiceList ? (NotifyServiceList)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnNotify(r.getArgument());
			}
			else {
			state = tempOut_state.outArgValue;
				Agent.logger.Warn("NotifyServiceList But SubscribeState Not Found.");
			}
			return Procedure.Success;
		}

		private int ProcessCommitServiceList(Protocol p) {
			var r = p instanceof CommitServiceList ? (CommitServiceList)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnCommit(r.getArgument());
			}
			else {
			state = tempOut_state.outArgValue;
				Agent.logger.Warn("CommitServiceList But SubscribeState Not Found.");
			}
			return Procedure.Success;
		}

		private int ProcessKeepalive(Protocol p) {
			var r = p instanceof Keepalive ? (Keepalive)p : null;
			if (getOnKeepAlive() != null) {
				getOnKeepAlive().run();
			}
			r.SendResultCode(Keepalive.Success);
			return Procedure.Success;
		}

		public final static class AutoKey {
			private String Name;
			public String getName() {
				return Name;
			}
			private long Current;
			public long getCurrent() {
				return Current;
			}
			private void setCurrent(long value) {
				Current = value;
			}
			private int Count;
			public int getCount() {
				return Count;
			}
			private void setCount(int value) {
				Count = value;
			}
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}

			public AutoKey(String name, Agent agent) {
				Name = name;
				Agent = agent;
			}

			public long Next() {
				synchronized (this) {
					if (getCount() <= 0) {
						Allocate();
					}

					if (getCount() <= 0) {
						throw new RuntimeException(String.format("AllocateId failed for %1$s", getName()));
					}

					var tmp = getCurrent();
					setCount(getCount() - 1);
					setCurrent(getCurrent() + 1);
					return tmp;
				}
			}

			private void Allocate() {
				var r = new AllocateId();
				r.getArgument().Name = getName();
				r.getArgument().setCount(1024);
				r.SendAndWaitCheckResultCode(getAgent().getClient().Socket);
				setCurrent(r.getResult().getStartId());
				setCount(r.getResult().getCount());
			}
		}

		private java.util.concurrent.ConcurrentHashMap<String, AutoKey> AutoKeys = new java.util.concurrent.ConcurrentHashMap<String, AutoKey> ();
		private java.util.concurrent.ConcurrentHashMap<String, AutoKey> getAutoKeys() {
			return AutoKeys;
		}

		public AutoKey GetAutoKey(String name) {
			return getAutoKeys().putIfAbsent(name, (k) -> new AutoKey(k, this));
		}

		public void Stop() {
			synchronized (this) {
				if (null == getClient()) {
					return;
				}
				getClient().Stop();
				setClient(null);
			}
		}

		public void OnConnected() {
			for (var e : getRegisters()) {
				try {
					var r = new Register();
					r.setArgument(e.Value);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException ex) {
					logger.Debug(ex, "OnConnected.Register={0}", e.Value);
				}
			}
			for (var e : getSubscribeStates()) {
				try {
					e.Value.Committed = false;
					var r = new Subscribe();
					r.setArgument(e.Value.SubscribeInfo);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException ex) {
					logger.Debug(ex, "OnConnected.Subscribe={0}", e.Value.SubscribeInfo);
				}
			}
		}

		/** 
		 使用Config配置连接信息，可以配置是否支持重连。
		 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
		*/

		public Agent(Config config) {
			this(config, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Agent(Config config, string netServiceName = null)
		public Agent(Config config, String netServiceName) {
			if (null == config) {
				throw new RuntimeException("Config is null");
			}

			setClient(tangible.StringHelper.isNullOrEmpty(netServiceName) ? new NetClient(this, config) : new NetClient(this, config, netServiceName));

			getClient().AddFactoryHandle((new Register()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Register()});

			getClient().AddFactoryHandle((new UnRegister()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnRegister()});

			getClient().AddFactoryHandle((new Subscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Subscribe()});

			getClient().AddFactoryHandle((new UnSubscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnSubscribe()});

			getClient().AddFactoryHandle((new NotifyServiceList()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new NotifyServiceList(), Handle = ProcessNotifyServiceList});

			getClient().AddFactoryHandle((new CommitServiceList()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new CommitServiceList(), Handle = ProcessCommitServiceList});

			getClient().AddFactoryHandle((new Keepalive()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Keepalive(), Handle = ProcessKeepalive});

			getClient().AddFactoryHandle((new SubscribeFirstCommit()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new SubscribeFirstCommit(), Handle = ProcessSubscribeFirstCommit});

			getClient().AddFactoryHandle((new AllocateId()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new AllocateId()});

			getClient().Start();
		}

		public void close() throws IOException {
			Stop();
		}

		public final static class NetClient extends HandshakeClient {
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}
			/** 
			 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
			*/
			private AsyncSocket Socket;
			public AsyncSocket getSocket() {
				return Socket;
			}
			private void setSocket(AsyncSocket value) {
				Socket = value;
			}

			public NetClient(Agent agent, Config config) {
				super("Zeze.Services.ServiceManager.Agent", config);
				Agent = agent;
			}
			public NetClient(Agent agent, Config config, String name) {
				super(name, config);
				Agent = agent;
			}

			@Override
			public void OnHandshakeDone(AsyncSocket sender) {
				super.OnHandshakeDone(sender);
				if (null == getSocket()) {
					setSocket(sender);
					Util.Task.Run(getAgent().OnConnected, "ServiceManager.Agent.OnConnected");
				}
				else {
					Agent.logger.Error("Has Connected.");
				}
			}

			@Override
			public void OnSocketClose(AsyncSocket so, Throwable e) {
				if (getSocket() == so) {
					setSocket(null);
				}
				super.OnSocketClose(so, e);
			}
		}
	}
}